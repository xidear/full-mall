package com.fullmall.platform.security;

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

// 顺序（+10）：先于 RateLimit 与 FeatureGuard，被它们拒掉的 403/429/503 响应也带齐安全头。
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    // app.security.hsts-enabled（env HSTS_ENABLED，默认 false，application-prod.yml 固定 true）。
    private final boolean hstsEnabled;

    public SecurityHeadersFilter(@Value("${app.security.hsts-enabled:false}") boolean hstsEnabled) {
        this.hstsEnabled = hstsEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 删掉后的症状：无 nosniff 可用上传文件诱导浏览器按 HTML/JS 执行；无 DENY 管理后台可被任意站点
        // iframe 嵌套（点击劫持）；CORP=same-site 允许同主域各前端引用素材、拦住第三方热链（有意）。
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(self)");
        // 商品图/素材可能由 CDN 或独立 API 域名提供：媒体路径允许跨源加载；
        // 其余接口保持 same-site，避免被第三方站点热链/嵌入。
        String path = request.getRequestURI();
        boolean publicMedia = path.startsWith("/images/") || path.startsWith("/assets/");
        response.setHeader("Cross-Origin-Resource-Policy", publicMedia ? "cross-origin" : "same-site");
        // 默认关闭：本地常用 http+IP 调试，而下发后浏览器长期强制 https+includeSubDomains 且无法立刻撤销，
        // 必须先确认全域（含子域）证书齐备再开。
        if (hstsEnabled) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        filterChain.doFilter(request, response);
    }
}