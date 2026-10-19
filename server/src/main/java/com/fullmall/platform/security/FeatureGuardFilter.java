package com.fullmall.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.feature.app.FeatureFlagService;
import com.fullmall.setting.app.SystemSettingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * 服务端功能开关与站点状态强制点（FLAG-001、FLAG-002、FLAG-003、FLAG-005）。
 *
 * <p>拦截规则：
 * <ul>
 *   <li>{@code supplier.enabled=false}：{@code /open/v1/**} 与 {@code /supplier/**} 返回 403。</li>
 *   <li>{@code pos.sale.enabled=false}：{@code /store/v1/pos/sales/**} 返回 403。</li>
 *   <li>{@code pos.pickup.enabled=false}：{@code /store/v1/pos/pickup/**} 返回 403。</li>
 *   <li>站点状态非 OPEN：顾客前台 {@code /api/v1/**} 返回 503，支付/退款回调、站点信息、
 *       admin/store/support/open/actuator 放行。</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class FeatureGuardFilter extends OncePerRequestFilter {

    private static final String PAYMENT_CALLBACK_PREFIX = "/api/v1/payments/";
    private static final String SITE_INFO_PREFIX = "/api/v1/site/";

    private final FeatureFlagService featureFlags;
    private final SystemSettingService settings;
    private final ObjectMapper objectMapper;

    public FeatureGuardFilter(FeatureFlagService featureFlags,
                              SystemSettingService settings,
                              ObjectMapper objectMapper) {
        this.featureFlags = featureFlags;
        this.settings = settings;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (path.startsWith("/open/") || path.startsWith("/supplier/")) {
            if (!featureFlags.isEnabled(FeatureFlagService.SUPPLIER_ENABLED)) {
                reject(request, response, HttpServletResponse.SC_FORBIDDEN, "SUPPLIER_DISABLED",
                        "供应商模块已关闭，请联系平台运营");
                return;
            }
        }

        if (path.startsWith("/store/v1/pos/sales")) {
            if (!featureFlags.isEnabled(FeatureFlagService.POS_SALE_ENABLED)) {
                reject(request, response, HttpServletResponse.SC_FORBIDDEN, "POS_SALE_DISABLED",
                        "POS 卖货功能已关闭");
                return;
            }
        }

        if (path.startsWith("/store/v1/pickups")) {
            if (!featureFlags.isEnabled(FeatureFlagService.POS_PICKUP_ENABLED)) {
                reject(request, response, HttpServletResponse.SC_FORBIDDEN, "POS_PICKUP_DISABLED",
                        "POS 自提核销功能已关闭");
                return;
            }
        }

        if (isCustomerFront(path)) {
            String status = settings.siteStatus();
            if (!SystemSettingService.SITE_STATUS_OPEN.equals(status)) {
                reject(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "SITE_" + status,
                        settings.maintenanceMessage());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 顾客前台判定：只包含商城/移动端使用的 {@code /api/v1/**}，
     * 并放行支付/退款回调与站点信息（关闭站点时前台仍需读取维护文案）。
     */
    private boolean isCustomerFront(String path) {
        if (!path.startsWith("/api/v1/")) {
            return false;
        }
        return !path.startsWith(PAYMENT_CALLBACK_PREFIX) && !path.startsWith(SITE_INFO_PREFIX);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, int status,
                        String code, String message) throws IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("X-Request-Id", requestId);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", code,
                "message", message == null || message.isBlank() ? code : message,
                "requestId", requestId));
    }
}