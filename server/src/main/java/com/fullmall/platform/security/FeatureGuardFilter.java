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

// 顺序（+30）：晚于 SecurityHeaders(+10)/RateLimit(+20)，这里拒掉的 403/503 也带安全头且已过限流；
// 早于 Spring Security 与 MemberAuthFilter/StaffAuthFilter(@Order(0))，"模块被关"不会被误报成登录问题。
// 注意 SupplierHmacFilter 同为 +20 且先执行，供应商模块关闭时未签名的 /supplier/** 会先拿到 401。
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

    // 回调必须放行：站点关闭时若把支付回调 503 掉，会出现"用户已付款、订单未标记已支付"的错账。
    private static final String PAYMENT_CALLBACK_PREFIX = "/api/v1/payments/";
    // 站点信息放行：维护页要靠它取站点名与维护文案。
    private static final String SITE_INFO_PREFIX = "/api/v1/site/";

    // 本类不缓存：两个依赖各带 app.feature.cache-ttl-seconds（默认 30 秒）缓存，故不打库，代价是改动延迟生效。
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
        // URI 含 contextPath（如反代把应用挂在 /mall 下），不剥掉则所有前缀规则静默失效、开关点了没反应。
        String path = request.getRequestURI().substring(request.getContextPath().length());

        // 门户与开放接口属同一能力；open 接口没有前端可隐藏入口，只能由后端给稳定码。
        if (path.startsWith("/open/") || path.startsWith("/supplier/")) {
            if (!featureFlags.isEnabled(FeatureFlagService.SUPPLIER_ENABLED)) {
                reject(request, response, HttpServletResponse.SC_FORBIDDEN, "SUPPLIER_DISABLED",
                        "供应商模块已关闭，请联系平台运营");
                return;
            }
        }

        // 卖货与自提是两个独立开关：门店可能只关卖货、仍保留自提履约。
        if (path.startsWith("/store/v1/pos/sales")) {
            if (!featureFlags.isEnabled(FeatureFlagService.POS_SALE_ENABLED)) {
                reject(request, response, HttpServletResponse.SC_FORBIDDEN, "POS_SALE_DISABLED",
                        "POS 卖货功能已关闭");
                return;
            }
        }

        // 实际前缀是 /store/v1/pickups（/store/v1/pos 只管卖货），以此处为准。
        if (path.startsWith("/store/v1/pickups")) {
            if (!featureFlags.isEnabled(FeatureFlagService.POS_PICKUP_ENABLED)) {
                reject(request, response, HttpServletResponse.SC_FORBIDDEN, "POS_PICKUP_DISABLED",
                        "POS 自提核销功能已关闭");
                return;
            }
        }

        // 只拦顾客前台：/admin、/store 等必须还能进，否则站点一关运营连"改回 OPEN"的页面都打不开；
        // 用 503 + SITE_<status> 而非 200 空数据，前台才能区分"维护"与"故障"。
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
        // 复用网关的 X-Request-Id，缺失才生成：用户凭 requestId 能反查这次拦截（AGENTS.md §10.5）。
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("X-Request-Id", requestId);
        // 过滤器在 MVC 之前，GlobalExceptionHandler 还没生效，只能手写 JSON；形状对齐 ErrorResponse。
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", code,
                "message", message == null || message.isBlank() ? code : message,
                "requestId", requestId));
    }
}