package com.fullmall.staff.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.staff.app.StaffAuthService;
import com.fullmall.staff.domain.StaffUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class StaffAuthFilter extends OncePerRequestFilter {

    public static final String STAFF_USER_ATTRIBUTE = "staffUser";
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "OPERATOR");

    private final StaffAuthService authService;
    private final ObjectMapper objectMapper;

    public StaffAuthFilter(StaffAuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/admin/")
                || path.equals("/admin/v1/auth/login")
                || path.equals("/admin/v1/auth/captcha")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = token(request.getHeader("Authorization"));
        StaffUser user = authService.me(token);
        if (user == null) {
            unauthorized(response, "UNAUTHORIZED", "请先登录运营后台");
            return;
        }
        if (!ADMIN_ROLES.contains(user.role())) {
            unauthorized(response, "FORBIDDEN", "当前账号没有运营后台权限");
            return;
        }

        request.setAttribute(STAFF_USER_ATTRIBUTE, user);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                user.username(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role()))));
        SecurityContextHolder.setContext(context);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String token(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }

    private void unauthorized(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        String requestId = UUID.randomUUID().toString();
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", code,
                "message", message,
                "requestId", requestId));
    }
}