package com.ecommerce.security;

import com.ecommerce.service.RiskEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SimpleRateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MILLIS = 60_000L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final RiskEventService riskEventService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        int limit = resolveLimit(request);
        if (limit > 0 && !allow(request, limit)) {
            riskEventService.recordRequest(request, "rate_limit_block", "high",
                    "接口访问频率过高，疑似刷单/恶意注册/高频爬虫",
                    "{\"limit\":" + limit + ",\"windowMillis\":" + WINDOW_MILLIS + "}");
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            OBJECT_MAPPER.writeValue(response.getWriter(), Map.of(
                    "code", 429,
                    "message", "操作太频繁，请稍后再试"
            ));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private int resolveLimit(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equals(method) && (uri.endsWith("/auth/login") || uri.endsWith("/auth/register"))) {
            return 20;
        }
        if ("POST".equals(method) && (uri.endsWith("/orders") || uri.endsWith("/upload/image") || uri.endsWith("/reviews"))) {
            return 30;
        }
        return 0;
    }

    private boolean allow(HttpServletRequest request, int limit) {
        cleanup();
        String key = clientIp(request) + ":" + request.getMethod() + ":" + request.getRequestURI();
        long now = Instant.now().toEpochMilli();
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(now));
        synchronized (bucket) {
            if (now - bucket.windowStart >= WINDOW_MILLIS) {
                bucket.windowStart = now;
                bucket.count = 0;
            }
            bucket.count++;
            return bucket.count <= limit;
        }
    }

    private void cleanup() {
        long now = Instant.now().toEpochMilli();
        buckets.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MILLIS * 2);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp.trim() : request.getRemoteAddr();
    }

    private static class Bucket {
        private long windowStart;
        private int count;
        private Bucket(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
