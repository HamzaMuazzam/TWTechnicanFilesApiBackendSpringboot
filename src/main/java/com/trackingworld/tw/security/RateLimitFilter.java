package com.trackingworld.tw.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.config.AppProperties;
import com.trackingworld.tw.common.web.ClientIp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory fixed-window limiter (per client IP, default 20 req/min) for the login endpoints:
 * /api/v1/app/auth/** and /api/v1/admin/auth/login. Answers 429 RATE_LIMITED.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TRACKED_IPS = 50_000;

    private final int limit;
    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties appProperties, ObjectMapper objectMapper) {
        this.limit = appProperties.rateLimit() != null ? appProperties.rateLimit().requestsPerMinute() : 20;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean limited = path.startsWith(ApiPaths.APP_AUTH + "/") || path.equals(ApiPaths.ADMIN_AUTH + "/login");
        return !limited || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = ClientIp.of(request);
        long now = System.currentTimeMillis();
        if (buckets.size() > MAX_TRACKED_IPS) {
            buckets.entrySet().removeIf(e -> e.getValue().isExpired(now));
        }
        Bucket bucket = buckets.computeIfAbsent(ip, k -> new Bucket(now));
        if (!bucket.tryAcquire(now, limit)) {
            log.warn("[AUTH][RATE-LIMIT] {} {} from {} exceeded {} req/min", request.getMethod(), request.getRequestURI(), ip, limit);
            response.setHeader("Retry-After", String.valueOf(Math.max(1, bucket.secondsUntilReset(now))));
            SecurityResponses.write(response, objectMapper, HttpStatus.TOO_MANY_REQUESTS.value(), ErrorCodes.RATE_LIMITED);
            return;
        }
        chain.doFilter(request, response);
    }

    static final class Bucket {
        private volatile long windowStart;
        private final AtomicInteger count = new AtomicInteger();

        Bucket(long now) {
            this.windowStart = now;
        }

        synchronized boolean tryAcquire(long now, int limit) {
            if (now - windowStart >= WINDOW_MS) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= limit;
        }

        boolean isExpired(long now) {
            return now - windowStart >= WINDOW_MS * 2;
        }

        long secondsUntilReset(long now) {
            return (WINDOW_MS - (now - windowStart)) / 1000;
        }
    }
}
