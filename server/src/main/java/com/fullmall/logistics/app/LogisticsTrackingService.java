package com.fullmall.logistics.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.risk.RiskAlertService;
import com.fullmall.setting.app.SystemSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 物流轨迹查询。
 *
 * <p>**演示用 mock，生产用后台配置**：`logistics_provider` 配置项决定实现
 * （`mock` / `kuaidi100`）。密钥走环境变量 `app.logistics.api-key`，**不进数据库、不进后台表单**。
 *
 * <p>**错误暴露策略（重要）**：供应商不可用时
 * <ul>
 *   <li>顾客只看到通用提示「物流信息暂时无法查询」，**不暴露内部原因**；</li>
 *   <li>内部维护人员通过风控告警（`risk_alert`）与错误日志获知具体原因。</li>
 * </ul>
 */
@Service
public class LogisticsTrackingService {

    private static final Logger log = LoggerFactory.getLogger(LogisticsTrackingService.class);
    private static final String PROVIDER_KEY = "logistics_provider";
    /** 按市场启用的通道（首批）；有启用项就以它为准，没有再回退全局设置。 */
    private final com.fullmall.market.app.MarketIntegrationService integrations;
    private static final String CUSTOMER_SAFE_MESSAGE = "物流信息暂时无法查询，请稍后再试";

    private final SystemSettingService settings;
    private final RiskAlertService riskAlerts;
    private final JdbcTemplate jdbcTemplate;
    private final String apiKey;
    /** 演示模式：无论后台配置了什么服务商，一律用 mock（不产生外网调用）。 */
    private final boolean demoForced;

    public LogisticsTrackingService(SystemSettingService settings, RiskAlertService riskAlerts,
                                    JdbcTemplate jdbcTemplate,
                                    com.fullmall.market.app.MarketIntegrationService integrations,
                                    @Value("${app.logistics.api-key:}") String apiKey,
                                    @Value("${app.mode:dev}") String appMode) {
        this.settings = settings;
        this.riskAlerts = riskAlerts;
        this.jdbcTemplate = jdbcTemplate;
        this.integrations = integrations;
        this.apiKey = apiKey;
        this.demoForced = "demo".equalsIgnoreCase(appMode);
    }

    public record Shipment(String carrierCode, String trackingNo) {
    }

    public record TrackingEvent(Instant at, String status, String location) {
    }

    /** 订单的物流轨迹（按履约单取承运商与运单号）。 */
    public List<TrackingEvent> trackOrder(UUID orderId) {
        List<Shipment> shipments = jdbcTemplate.query("""
                SELECT carrier_code, tracking_no FROM fulfillment_order
                 WHERE order_id = ? AND tracking_no IS NOT NULL
                 ORDER BY created_at
                """, (rs, rowNum) -> new Shipment(
                rs.getString("carrier_code"), rs.getString("tracking_no")), orderId);

        if (shipments.isEmpty()) {
            throw ApiException.notFound("TRACKING_NOT_FOUND", "该订单还没有物流信息");
        }

        // 服务商按**订单所属市场**取：香港站不应使用大陆配置的物流服务商
        List<String> markets = jdbcTemplate.queryForList(
                "SELECT market_code FROM orders WHERE id = ?", String.class, orderId);
        String marketCode = markets.isEmpty() ? null : markets.get(0);
        List<TrackingEvent> events = new ArrayList<>();
        for (Shipment shipment : shipments) {
            events.addAll(track(shipment, marketCode));
        }
        return events;
    }

    @Transactional
    public List<TrackingEvent> track(Shipment shipment) {
        return track(shipment, null);
    }

    /** @param marketCode 订单所属市场；为 null 时退回全局配置（无市场上下文的老调用方） */
    public List<TrackingEvent> track(Shipment shipment, String marketCode) {
        if (demoForced) {
            // 演示环境永远 mock：即便后台填了真实服务商与 AK，也不发外网请求
            return mockEvents(shipment);
        }
        // 优先用该市场启用的通道；没有启用项才回退全局设置（迁移期兼容，不是永久双写）
        String provider = integrations.enabledProvider(marketCode, "logistics")
                .orElseGet(() -> {
                    String legacy = settings.marketValue(marketCode, PROVIDER_KEY);
                    return legacy == null || legacy.isBlank() ? "mock" : legacy.trim().toLowerCase();
                });
        provider = provider == null || provider.isBlank() ? "mock" : provider.trim().toLowerCase();
        return switch (provider) {
            case "mock" -> mockEvents(shipment);
            case "kuaidi100" -> kuaidi100Events(shipment);
            default -> unavailable(provider, shipment, "未知的 logistics_provider: " + provider);
        };
    }

    /** 演示轨迹：不依赖外部服务，结果稳定可预期。 */
    private List<TrackingEvent> mockEvents(Shipment shipment) {
        Instant now = Instant.now();
        return List.of(
                new TrackingEvent(now.minus(Duration.ofHours(6)), "已揽收", "上海市分拣中心"),
                new TrackingEvent(now.minus(Duration.ofHours(3)), "运输中", "杭州转运中心"),
                new TrackingEvent(now.minus(Duration.ofMinutes(30)), "派送中", "目的地网点"));
    }

    private List<TrackingEvent> kuaidi100Events(Shipment shipment) {
        // AK 由后台填写（logistics_key），环境变量作为兜底
        String customer = settings.rawValue("logistics_customer");
        String key = settings.rawValue("logistics_key");
        if (key == null || key.isBlank()) {
            key = apiKey;
        }
        if (key == null || key.isBlank()) {
            return unavailable("kuaidi100", shipment, "未配置物流服务商 AK（后台「物流 Key」）");
        }
        if (customer == null || customer.isBlank()) {
            return unavailable("kuaidi100", shipment, "未配置快递100 customer（后台「物流 Customer」）");
        }
        // 请求体构造已完成并可单测；真正的 HTTP 调用需要 AK 才能联调，这里显式失败而不是假装成功
        Kuaidi100Query.Form form = Kuaidi100Query.build(customer, key,
                shipment.carrierCode(), shipment.trackingNo());
        return unavailable("kuaidi100", shipment,
                "快递100 请求已构造（sign=" + form.sign().substring(0, 8) + "…），"
                        + "但未配置联调环境，未发起网络调用");
    }

    /**
     * 供应方不可用：**内部告警 + 顾客通用提示**。
     *
     * <p>这是刻意区分：顾客看到「暂时无法查询」，维护人员在风控台能看到具体原因。
     */
    private List<TrackingEvent> unavailable(String provider, Shipment shipment, String internalReason) {
        log.error("物流轨迹查询不可用 provider={} carrier={} tracking={} reason={}",
                provider, shipment.carrierCode(), shipment.trackingNo(), internalReason);
        riskAlerts.raise("LOGISTICS_PROVIDER_UNAVAILABLE", RiskAlertService.Severity.MEDIUM,
                shipment.trackingNo() == null ? "unknown" : shipment.trackingNo(),
                internalReason, Duration.ofMinutes(10));
        // 顾客侧只给通用提示（不暴露内部原因）；内部原因已进告警与日志
        throw new com.fullmall.platform.error.ApiException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "LOGISTICS_UNAVAILABLE", CUSTOMER_SAFE_MESSAGE);
    }
}
