package com.fullmall.staff.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.permission.app.PermissionService;
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
    private static final Set<String> STORE_ROLES = Set.of("ADMIN", "OPERATOR", "STORE");
    private static final Set<String> SUPPORT_ROLES = Set.of("ADMIN", "OPERATOR", "CS");

    private final StaffAuthService authService;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;
    private final boolean storeAuthRequired;
    private final boolean supportAuthRequired;

    public StaffAuthFilter(StaffAuthService authService, PermissionService permissionService,
                           ObjectMapper objectMapper,
                           @org.springframework.beans.factory.annotation.Value("${app.store.auth-required:false}")
                           boolean storeAuthRequired,
                           @org.springframework.beans.factory.annotation.Value("${app.support.auth-required:false}")
                           boolean supportAuthRequired) {
        this.authService = authService;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
        this.storeAuthRequired = storeAuthRequired;
        this.supportAuthRequired = supportAuthRequired;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/store/")) {
            return !storeAuthRequired;
        }
        if (path.startsWith("/support/")) {
            return !supportAuthRequired;
        }
        return !path.startsWith("/admin/")
                || path.equals("/admin/v1/auth/login")
                || path.equals("/admin/v1/auth/captcha");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean storeRequest = path.startsWith("/store/");
        boolean supportRequest = path.startsWith("/support/");
        Set<String> allowedRoles = supportRequest ? SUPPORT_ROLES : storeRequest ? STORE_ROLES : ADMIN_ROLES;
        String token = token(request.getHeader("Authorization"));
        StaffUser user = authService.me(token);
        if (user == null) {
            unauthorized(response, "UNAUTHORIZED",
                    supportRequest ? "请先登录客服工作台"
                            : storeRequest ? "请先登录门店工作台" : "请先登录运营后台");
            return;
        }
        if (!allowedRoles.contains(user.role())) {
            unauthorized(response, "FORBIDDEN",
                    supportRequest ? "当前账号没有客服工作台权限"
                            : storeRequest ? "当前账号没有门店工作台权限" : "当前账号没有运营后台权限");
            return;
        }
        if (!storeRequest && !supportRequest) {
            String required = permissionService.requiredPermission(path);
            if (!permissionService.hasPermission(user.role(), required)) {
                forbidden(response, "PERMISSION_DENIED",
                        "当前账号没有该操作权限：" + required);
                return;
            }
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

    private void forbidden(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        String requestId = UUID.randomUUID().toString();
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", code,
                "message", message,
                "requestId", requestId));
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