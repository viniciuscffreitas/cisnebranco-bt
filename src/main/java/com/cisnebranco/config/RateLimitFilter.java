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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final int requestsPerMinute;
    private final int authRequestsPerMinute;
    private final List<String> trustedProxyCidrs;
    private final ObjectMapper objectMapper;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            int requestsPerMinute,
            int authRequestsPerMinute,
            List<String> trustedProxyCidrs,
            ObjectMapper objectMapper) {
        this.requestsPerMinute = requestsPerMinute;
        this.authRequestsPerMinute = authRequestsPerMinute;
        this.trustedProxyCidrs = trustedProxyCidrs;
        this.objectMapper = objectMapper;
        log.info("Rate limiting enabled: {} req/min general, {} req/min auth, trusted proxies: {}",
                requestsPerMinute, authRequestsPerMinute, trustedProxyCidrs);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        String path = request.getServletPath();

        boolean isAuth = path.startsWith("/auth/");
        int limit = isAuth ? authRequestsPerMinute : requestsPerMinute;
        String key = clientIp + ":" + (isAuth ? "auth" : "general");

        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(limit));

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
     * Resolves the real client IP. When the direct TCP connection comes from a trusted proxy
     * (e.g. Nginx Proxy Manager running in Docker), the first entry of X-Forwarded-For is used.
     * This prevents rate-limit bypass: untrusted direct connections cannot inject a fake
     * X-Forwarded-For because their remoteAddr will not match any trusted CIDR.
     */
    String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (isTrustedProxy(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                // Format: client, proxy1, proxy2 — first element is the real client IP
                String clientIp = xForwardedFor.split(",")[0].trim();
                if (!clientIp.isBlank()) {
                    return clientIp;
                }
            }
        }

        return remoteAddr;
    }

    private boolean isTrustedProxy(String ip) {
        return trustedProxyCidrs.stream().anyMatch(cidr -> matchesCidr(ip, cidr));
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
        } catch (Exception e) {
            return false;
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
