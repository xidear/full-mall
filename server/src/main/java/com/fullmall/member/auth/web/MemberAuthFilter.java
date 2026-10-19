package com.fullmall.member.auth.web;

import com.fullmall.member.auth.MemberAuthService;
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

/**
 * 会员 token 鉴权（仅作用于顾客前台 `/api/v1/**`）。
 *
 * <p>携带 `Authorization: Bearer <member token>` 时解析为会员身份，principal 名 = 会员 UUID，
 * 于是 {@code OwnerKeyResolver} 得到 `member:<uuid>`；不带 token 时保持游客语义，不影响现有行为。
 *
 * <p>无效/过期 token **不静默降级为游客**：直接 401，避免"以为已登录、实际下到别人账号"。
 */
@Component
@Order(0)  // 必须晚于 Spring Security 的过滤器链（其链顺序为 -100）
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class MemberAuthFilter extends OncePerRequestFilter {

    private final MemberAuthService auth;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public MemberAuthFilter(MemberAuthService auth,
                            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.auth = auth;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 只跳过登录接口本身；/api/v1/member/auth/me 等仍需鉴权
        return !path.startsWith("/api/v1/")
                || path.equals("/api/v1/member/auth/otp")
                || path.equals("/api/v1/member/auth/verify");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring("Bearer ".length()).trim();
        var memberId = auth.resolveSession(token);
        if (memberId.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(response.getWriter(), java.util.Map.of(
                    "code", "MEMBER_SESSION_INVALID",
                    "message", "登录已失效，请重新登录"));
            return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                memberId.get().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))));
        SecurityContextHolder.setContext(context);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
