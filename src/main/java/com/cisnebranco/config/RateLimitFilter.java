package com.cisnebranco.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cisnebranco.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final int requestsPerMinute;
    private final int authRequestsPerMinute;
    private final ObjectMapper objectMapper;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.requests-per-minute:60}") int requestsPerMinute,
            @Value("${app.rate-limit.auth-requests-per-minute:10}") int authRequestsPerMinute,
            ObjectMapper objectMapper) {
        this.requestsPerMinute = requestsPerMinute;
        this.authRequestsPerMinute = authRequestsPerMinute;
        this.objectMapper = objectMapper;
        log.info("Rate limiting enabled: {} req/min general, {} req/min auth", requestsPerMinute, authRequestsPerMinute);
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

    private String getClientIp(HttpServletRequest request) {
        // Use remoteAddr as the primary key — it's set by the reverse proxy (NPM)
        // and cannot be spoofed by the client
        return request.getRemoteAddr();
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
