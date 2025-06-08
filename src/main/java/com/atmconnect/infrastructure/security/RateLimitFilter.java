package com.atmconnect.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {
    
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_AUTH_REQUESTS_PER_MINUTE = 5;
    private static final int WINDOW_SIZE_SECONDS = 60;
    
    private final ConcurrentHashMap<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    private static class RateLimitEntry {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private final AtomicInteger authRequestCount = new AtomicInteger(0);
        private volatile long windowStart = Instant.now().getEpochSecond();
        
        boolean isWindowExpired() {
            return (Instant.now().getEpochSecond() - windowStart) >= WINDOW_SIZE_SECONDS;
        }
        
        void resetWindow() {
            windowStart = Instant.now().getEpochSecond();
            requestCount.set(0);
            authRequestCount.set(0);
        }
        
        boolean exceedsLimit(boolean isAuthRequest) {
            if (isWindowExpired()) {
                resetWindow();
            }
            
            int currentRequests = requestCount.incrementAndGet();
            if (isAuthRequest) {
                int currentAuthRequests = authRequestCount.incrementAndGet();
                return currentAuthRequests > MAX_AUTH_REQUESTS_PER_MINUTE || 
                       currentRequests > MAX_REQUESTS_PER_MINUTE;
            }
            
            return currentRequests > MAX_REQUESTS_PER_MINUTE;
        }
        
        int getRemainingRequests(boolean isAuthRequest) {
            if (isAuthRequest) {
                return Math.max(0, MAX_AUTH_REQUESTS_PER_MINUTE - authRequestCount.get());
            }
            return Math.max(0, MAX_REQUESTS_PER_MINUTE - requestCount.get());
        }
        
        long getWindowReset() {
            return windowStart + WINDOW_SIZE_SECONDS;
        }
    }\n    \n    public RateLimitFilter() {\n        // Start cleanup task to remove expired entries\n        cleanupExecutor.scheduleAtFixedRate(() -> {\n            requestCounts.entrySet().removeIf(entry -> entry.getValue().isWindowExpired());\n        }, 1, 1, TimeUnit.MINUTES);\n    }\n    \n    @Override\n    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, \n                                  FilterChain filterChain) throws ServletException, IOException {\n        \n        String clientIdentifier = getClientIdentifier(request);\n        boolean isAuthRequest = isAuthenticationRequest(request);\n        \n        RateLimitEntry entry = requestCounts.computeIfAbsent(clientIdentifier, k -> new RateLimitEntry());\n        \n        if (entry.exceedsLimit(isAuthRequest)) {\n            handleRateLimitExceeded(response, entry, isAuthRequest);\n            return;\n        }\n        \n        // Add rate limit headers\n        addRateLimitHeaders(response, entry, isAuthRequest);\n        \n        filterChain.doFilter(request, response);\n    }\n    \n    private String getClientIdentifier(HttpServletRequest request) {\n        // Try to get real IP address\n        String xForwardedFor = request.getHeader(\"X-Forwarded-For\");\n        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {\n            return xForwardedFor.split(\",\")[0].trim();\n        }\n        \n        String xRealIp = request.getHeader(\"X-Real-IP\");\n        if (xRealIp != null && !xRealIp.isEmpty()) {\n            return xRealIp;\n        }\n        \n        return request.getRemoteAddr();\n    }\n    \n    private boolean isAuthenticationRequest(HttpServletRequest request) {\n        String path = request.getRequestURI();\n        String method = request.getMethod();\n        \n        return (path.startsWith(\"/api/v1/auth/\") && \"POST\".equals(method)) ||\n               path.contains(\"/login\") || path.contains(\"/register\") || path.contains(\"/verify\");\n    }\n    \n    private void handleRateLimitExceeded(HttpServletResponse response, RateLimitEntry entry, \n                                       boolean isAuthRequest) throws IOException {\n        response.setStatus(429); // Too Many Requests\n        response.setContentType(\"application/json\");\n        response.setHeader(\"Retry-After\", String.valueOf(WINDOW_SIZE_SECONDS));\n        response.setHeader(\"X-RateLimit-Limit\", \n            String.valueOf(isAuthRequest ? MAX_AUTH_REQUESTS_PER_MINUTE : MAX_REQUESTS_PER_MINUTE));\n        response.setHeader(\"X-RateLimit-Remaining\", \"0\");\n        response.setHeader(\"X-RateLimit-Reset\", String.valueOf(entry.getWindowReset()));\n        \n        String errorMessage = isAuthRequest ? \n            \"Authentication rate limit exceeded. Please wait before trying again.\" :\n            \"Rate limit exceeded. Too many requests.\";\n            \n        response.getWriter().write(String.format(\n            \"{\\\"error\\\":\\\"Rate Limit Exceeded\\\",\\\"message\\\":\\\"%s\\\",\\\"retryAfter\\\":%d}\",\n            errorMessage, WINDOW_SIZE_SECONDS));\n        \n        log.warn(\"Rate limit exceeded for client: {}, auth request: {}\", \n            getClientIdentifier(null), isAuthRequest);\n    }\n    \n    private void addRateLimitHeaders(HttpServletResponse response, RateLimitEntry entry, \n                                   boolean isAuthRequest) {\n        int limit = isAuthRequest ? MAX_AUTH_REQUESTS_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;\n        int remaining = entry.getRemainingRequests(isAuthRequest);\n        \n        response.setHeader(\"X-RateLimit-Limit\", String.valueOf(limit));\n        response.setHeader(\"X-RateLimit-Remaining\", String.valueOf(remaining));\n        response.setHeader(\"X-RateLimit-Reset\", String.valueOf(entry.getWindowReset()));\n    }\n    \n    @Override\n    public void destroy() {\n        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {\n            cleanupExecutor.shutdown();\n            try {\n                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {\n                    cleanupExecutor.shutdownNow();\n                }\n            } catch (InterruptedException e) {\n                cleanupExecutor.shutdownNow();\n                Thread.currentThread().interrupt();\n            }\n        }\n    }\n}