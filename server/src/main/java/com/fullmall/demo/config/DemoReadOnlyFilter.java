package com.fullmall.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("demo")
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class DemoReadOnlyFilter extends OncePerRequestFilter {

    private final boolean readOnly;
    private final ObjectMapper objectMapper;

    public DemoReadOnlyFilter(@Value("${app.demo.read-only:true}") boolean readOnly,
                              ObjectMapper objectMapper) {
        this.readOnly = readOnly;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!readOnly || isReadOnlyMethod(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Allow", "GET, HEAD, OPTIONS");
        response.setHeader("X-Request-Id", requestId);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", "DEMO_READ_ONLY",
                "message", "The public demo is read-only. Use GET endpoints or /mock/**.",
                "requestId", requestId));
    }

    private boolean isReadOnlyMethod(String method) {
        return "GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method);
    }
}