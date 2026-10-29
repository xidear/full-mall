package com.fullmall.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// 顺序（+20）：早于 FeatureGuard 与 Spring Security，把过密的写请求挡在鉴权与数据库之前。
/**
 * Single-node write limiter. It is a hard guard for the open-source/demo build;
 * a Valkey-backed limiter should replace it when the API runs multiple replicas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RateLimitFilter extends OncePerRequestFilter {

    // app.rate-limit.enabled / max-requests / window-seconds（env RATE_LIMIT_*，默认 120 次/60 秒）。
    // 窗口从该桶首个写请求起算；max-requests 配成 0 或负数等于封掉全部写请求。
    private final boolean enabled;
    private final int maxRequests;
    private final long windowMillis;
    private final ObjectMapper objectMapper;
    // 单机内存计数且从不清理（重启归零）；多副本时各副本独立计数，真实限额放大成 max-requests×副本数，
    // 且判定不一致，需换 Valkey 共享计数（见类注释）。
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.enabled:true}") boolean enabled,
                           @Value("${app.rate-limit.max-requests:120}") int maxRequests,
                           @Value("${app.rate-limit.window-seconds:60}") long windowSeconds,
                           ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled || !isWrite(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 按 IP+方法+路径分桶：避免一人吃光全站配额，也不让不同接口互相挤占；
        // 代价是同一 NAT 出口后的多个用户共用一个桶。
        String key = request.getRemoteAddr() + ":" + request.getMethod() + ":" + request.getRequestURI();
        Window window = windows.computeIfAbsent(key, ignored -> new Window(System.currentTimeMillis(), 0));
        long now = System.currentTimeMillis();
        int current;
        long resetInSeconds;
        // 计数必须在锁内：并发下否则会少算、漏拒；锁不含响应写出与下游调用，慢请求不会卡住同桶请求。
        synchronized (window) {
            if (now - window.start >= windowMillis) {
                window.start = now;
                window.count = 0;
            }
            window.count++;
            current = window.count;
            resetInSeconds = Math.max(1, (window.start + windowMillis - now) / 1000);
        }

        // 429 + Retry-After + X-RateLimit-*：让调用方知道多久后重试；错误码 RATE_LIMITED 供前端映射文案。
        if (current > maxRequests) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After", String.valueOf(resetInSeconds));
            response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
            response.setHeader("X-RateLimit-Remaining", "0");
            String requestId = request.getHeader("X-Request-Id");
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "code", "RATE_LIMITED",
                    "message", "Too many write requests. Retry later.",
                    "requestId", requestId));
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, maxRequests - current)));
        filterChain.doFilter(request, response);
    }

    private boolean isWrite(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    // 字段可见性靠外层 synchronized(window)；只存在于当前 JVM，不能当跨实例配额或计费依据。
    private static final class Window {
        private long start;
        private int count;

        private Window(long start, int count) {
            this.start = start;
            this.count = count;
        }
    }
}