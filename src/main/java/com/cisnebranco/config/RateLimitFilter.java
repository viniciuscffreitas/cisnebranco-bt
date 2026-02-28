package com.cisnebranco.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cisnebranco.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Pattern UPLOAD_PATTERN =
            Pattern.compile("/os/\\d+/(photos|checklist)(/.*)?");

    private final int requestsPerMinute;
    private final int authRequestsPerMinute;
    private final int uploadRequestsPerMinute;
    private final int reportRequestsPerMinute;
    private final List<String> trustedProxyCidrs;
    private final ObjectMapper objectMapper;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            int requestsPerMinute,
            int authRequestsPerMinute,
            int uploadRequestsPerMinute,
            int reportRequestsPerMinute,
            List<String> trustedProxyCidrs,
            ObjectMapper objectMapper) {
        this.requestsPerMinute = requestsPerMinute;
        this.authRequestsPerMinute = authRequestsPerMinute;
        this.uploadRequestsPerMinute = uploadRequestsPerMinute;
        this.reportRequestsPerMinute = reportRequestsPerMinute;
        this.trustedProxyCidrs = trustedProxyCidrs;
        this.objectMapper = objectMapper;

        // Validate CIDRs at startup — bad config must fail fast, not silently at runtime
        for (String cidr : trustedProxyCidrs) {
            validateCidr(cidr);
        }

        if (trustedProxyCidrs.isEmpty()) {
            log.warn("No trusted proxy CIDRs configured (RATE_LIMIT_TRUSTED_CIDRS). " +
                     "X-Forwarded-For will be ignored; all traffic rate-limited by direct connection IP. " +
                     "Set RATE_LIMIT_TRUSTED_CIDRS if running behind a reverse proxy.");
        }

        log.info("Rate limiting enabled: {} req/min general, {} req/min auth, {} req/min upload, {} req/min report, trusted proxies: {}",
                requestsPerMinute, authRequestsPerMinute, uploadRequestsPerMinute, reportRequestsPerMinute, trustedProxyCidrs);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        String path = request.getServletPath();

        RateCategory category = resolveCategory(path);
        String key = clientIp + ":" + category.name();

        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(category.limit()));

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded: ip={}, path={}, key={}", clientIp, path, key);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            try {
                ApiError error = new ApiError(429, "Too many requests. Please try again later.");
                response.getWriter().write(objectMapper.writeValueAsString(error));
            } catch (IOException e) {
                log.warn("Failed to write 429 response for ip={}: {}", clientIp, e.getMessage());
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/actuator/") || path.startsWith("/swagger-ui/") || path.startsWith("/api-docs/");
    }

    /**
     * Resolves the rate-limit category and limit for a given servlet path.
     * Categories are checked in specificity order: most restrictive patterns first.
     */
    record RateCategory(String name, int limit) {}

    RateCategory resolveCategory(String path) {
        if (path.startsWith("/auth/")) {
            return new RateCategory("auth", authRequestsPerMinute);
        }
        if (UPLOAD_PATTERN.matcher(path).matches()) {
            return new RateCategory("upload", uploadRequestsPerMinute);
        }
        if (path.startsWith("/reports/") || path.equals("/reports")) {
            return new RateCategory("report", reportRequestsPerMinute);
        }
        return new RateCategory("general", requestsPerMinute);
    }

    /**
     * Resolves the real client IP. When the direct TCP connection comes from a trusted proxy
     * (e.g. Nginx Proxy Manager running in Docker), the first entry of X-Forwarded-For is used.
     * This prevents rate-limit bypass: untrusted direct connections cannot inject a fake
     * X-Forwarded-For because their remoteAddr will not match any trusted CIDR.
     *
     * The extracted IP is validated to be a numeric address to prevent hostname injection
     * (which would otherwise trigger DNS lookups on the hot path).
     */
    String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (isTrustedProxy(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                // Format: client, proxy1, proxy2 — first element is the real client IP
                String rawIp = xForwardedFor.split(",")[0].trim();
                if (!rawIp.isBlank()) {
                    if (isNumericIpAddress(rawIp)) {
                        log.debug("getClientIp: trusted proxy {} -> client ip={}", remoteAddr, rawIp);
                        return rawIp;
                    } else {
                        log.warn("getClientIp: X-Forwarded-For contains non-numeric value '{}' " +
                                 "from trusted proxy {}, falling back to remoteAddr", rawIp, remoteAddr);
                    }
                }
            }
        }

        return remoteAddr;
    }

    private boolean isTrustedProxy(String ip) {
        return trustedProxyCidrs.stream().anyMatch(cidr -> matchesCidr(ip, cidr));
    }

    /**
     * Returns true if the given string looks like a numeric IPv4 or IPv6 address.
     * Uses a simple character-class check rather than InetAddress.getByName() to
     * avoid blocking DNS lookups on the request-handling thread for hostname strings.
     */
    static boolean isNumericIpAddress(String s) {
        if (s == null || s.isBlank()) return false;
        // IPv4: digits and dots only (e.g. "203.0.113.42")
        if (s.matches("[\\d.]+")) return true;
        // IPv6: hex digits, colons, and optional scope ID with % (e.g. "::1", "fe80::1%eth0")
        return s.matches("[0-9a-fA-F:%]+");
    }

    static boolean matchesCidr(String ip, String cidr) {
        try {
            if (!cidr.contains("/")) {
                return cidr.equals(ip);
            }
            String[] parts = cidr.split("/", 2);
            byte[] cidrBytes = InetAddress.getByName(parts[0]).getAddress();
            byte[] ipBytes = InetAddress.getByName(ip).getAddress();
            if (cidrBytes.length != ipBytes.length) return false;
            int prefixLen = Integer.parseInt(parts[1]);
            int bitsLeft = prefixLen;
            for (int i = 0; i < cidrBytes.length && bitsLeft > 0; i++) {
                int bits = Math.min(8, bitsLeft);
                int mask = 0xFF & (0xFF << (8 - bits));
                if ((cidrBytes[i] & mask) != (ipBytes[i] & mask)) return false;
                bitsLeft -= bits;
            }
            return true;
        } catch (UnknownHostException e) {
            log.warn("matchesCidr: invalid address in CIDR '{}' or IP '{}': {}", cidr, ip, e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            log.warn("matchesCidr: invalid prefix length in CIDR '{}': {}", cidr, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("matchesCidr: unexpected error comparing ip='{}' with cidr='{}': {}",
                    ip, cidr, e.getMessage(), e);
            return false;
        }
    }

    private static void validateCidr(String cidr) {
        try {
            if (cidr.contains("/")) {
                String[] parts = cidr.split("/", 2);
                InetAddress.getByName(parts[0]);
                int prefixLen = Integer.parseInt(parts[1]);
                if (prefixLen < 0 || prefixLen > 128) {
                    throw new IllegalArgumentException(
                            "Prefix length out of range [0, 128]: " + prefixLen);
                }
            } else {
                InetAddress.getByName(cidr);
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(
                    "Invalid host in trusted proxy CIDR '" + cidr + "': " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid prefix length in trusted proxy CIDR '" + cidr + "': " + e.getMessage(), e);
        }
    }

    void evictExpiredBuckets() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, TokenBucket>> it = buckets.entrySet().iterator();
        int removed = 0;
        while (it.hasNext()) {
            Map.Entry<String, TokenBucket> entry = it.next();
            if (now - entry.getValue().getLastActivityTime() > 300_000) { // 5 minutes idle
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Evicted {} idle rate limit buckets, {} remaining", removed, buckets.size());
        }
    }

    private static class TokenBucket {
        private final int maxTokens;
        private long tokens;
        private long lastRefillTime;
        private long lastActivityTime;

        TokenBucket(int maxTokens) {
            this.maxTokens = maxTokens;
            this.tokens = maxTokens;
            long now = System.currentTimeMillis();
            this.lastRefillTime = now;
            this.lastActivityTime = now;
        }

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            lastActivityTime = now;

            if (now - lastRefillTime >= 60_000) {
                tokens = maxTokens;
                lastRefillTime = now;
            }

            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        synchronized long getLastActivityTime() {
            return lastActivityTime;
        }
    }
}
