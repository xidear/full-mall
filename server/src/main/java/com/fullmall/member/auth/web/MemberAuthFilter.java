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
    private final com.fullmall.staff.app.StaffAuthService staffAuth;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public MemberAuthFilter(MemberAuthService auth,
                            com.fullmall.staff.app.StaffAuthService staffAuth,
                            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.auth = auth;
        this.staffAuth = staffAuth;
        this.objectMapper = objectMapper;
    }

    @Override
    // 用原始 requestURI（含 context path）判断：一旦部署在非根 context-path 或网关注入了前缀，这里会整段跳过，
    // 症状是"带着 token 仍被当游客"，而不是 401
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
        // 只认 Authorization: Bearer，不读 cookie：cookie 化会同时改变 CSRF 暴露面，token 存哪里由前端决定
        String header = request.getHeader("Authorization");
        // 无 token 即游客；"必须登录"的接口由各自 controller/service 判断，这里不兜
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring("Bearer ".length()).trim();
        var memberId = auth.resolveSession(token);
        if (memberId.isEmpty()) {
            // 后台页面会复用顾客端接口（/api/v1/theme、/api/v1/stores、/api/v1/products 等），
            // 携带的是员工 token。它不是会员 token，但也不该被判成"登录已失效"——
            // 这里识别员工会话后按员工身份放行，只有在两种会话都解析不出来时才 401。
            var staff = staffAuth.me(token);
            if (staff == null) {
                // 顾客端只给稳定错误码 + 一句人话：不区分"过期/伪造/已登出"，也不回 WWW-Authenticate 等内部细节
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                objectMapper.writeValue(response.getWriter(), java.util.Map.of(
                        "code", "MEMBER_SESSION_INVALID",
                        "message", "登录已失效，请重新登录"));
                return;
            }
            SecurityContext staffContext = SecurityContextHolder.createEmptyContext();
            staffContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                    staff.username(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + staff.role()))));
            SecurityContextHolder.setContext(staffContext);
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
            return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                memberId.get().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))));
        SecurityContextHolder.setContext(context);
        // 线程池会复用线程：不 clear 就让下一个请求继承本请求的 principal（表现为越权读到别人的订单）
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
