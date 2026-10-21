package com.fullmall.licensing.web;

import com.fullmall.licensing.app.LicenseService;
import com.fullmall.licensing.app.LicenseStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 到期只读闸门：许可证进入只读模式后，**只放行读请求**，写请求返回 402。
 *
 * <p>放行清单（都必须 POST，否则会导致业务无法恢复或资金丢失）：
 * <ul>
 *   <li>登录/验证码/OTP：否则运维连后台都进不去，交不了续费；</li>
 *   <li>许可证上传与状态：否则无法恢复；</li>
 *   <li>**支付回调**：到期不该吞掉已发生的付款（否则客户真金白银丢了）。这是刻意的例外，
 *       不是遗漏。</li>
 * </ul>
 *
 * <p>其余 POST/PUT/PATCH/DELETE 一律 402 `LICENSE_EXPIRED`，响应带续费说明。
 */
@Component
@Order(-50)
public class LicenseReadOnlyFilter extends OncePerRequestFilter {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private static final List<String> ALWAYS_ALLOWED = List.of(
            "/admin/v1/auth/",
            "/admin/v1/license/",
            "/api/v1/member/auth/",
            "/api/v1/payments/",        // 支付/退款回调：不允许因到期丢单
            "/actuator/health"
    );

    private final LicenseService licenseService;

    public LicenseReadOnlyFilter(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        LicenseService.setCurrentHost(request.getServerName());
        try {
            LicenseStatus status = licenseService.status();
            if (!status.readOnly() || READ_METHODS.contains(request.getMethod())
                    || isAllowed(request.getRequestURI())) {
                chain.doFilter(request, response);
                return;
            }
            response.setStatus(402);   // Payment Required：语义就是"该付费/续费了"
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {"code":"LICENSE_EXPIRED","message":"%s","hint":"许可证到期后为只读模式，\
                    可查看与导出数据；请上传新许可证或续费后继续操作。"}
                    """.formatted(status.message().replace("\"", "'")));
        } finally {
            LicenseService.clearCurrentHost();
        }
    }

    private boolean isAllowed(String uri) {
        return ALWAYS_ALLOWED.stream().anyMatch(uri::startsWith);
    }
}
