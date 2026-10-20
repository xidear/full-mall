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
    private static final String CUSTOMER_SAFE_MESSAGE = "物流信息暂时无法查询，请稍后再试";

    private final SystemSettingService settings;
    private final RiskAlertService riskAlerts;
    private final JdbcTemplate jdbcTemplate;
    private final String apiKey;

    public LogisticsTrackingService(SystemSettingService settings, RiskAlertService riskAlerts,
                                    JdbcTemplate jdbcTemplate,
                                    @Value("${app.logistics.api-key:}") String apiKey) {
        this.settings = settings;
        this.riskAlerts = riskAlerts;
        this.jdbcTemplate = jdbcTemplate;
        this.apiKey = apiKey;
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

        List<TrackingEvent> events = new ArrayList<>();
        for (Shipment shipment : shipments) {
            events.addAll(track(shipment));
        }
        return events;
    }

    @Transactional
    public List<TrackingEvent> track(Shipment shipment) {
        String provider = settings.adminSettings().getOrDefault(PROVIDER_KEY, "mock").trim().toLowerCase();
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
        if (apiKey == null || apiKey.isBlank()) {
            return unavailable("kuaidi100", shipment, "缺少 app.logistics.api-key 配置");
        }
        // 真实对接需要快递100的签名与回调参数，本构建未接（无凭据无法验证），显式失败而非假装成功
        return unavailable("kuaidi100", shipment,
                "kuaidi100 适配器尚未实现（缺少联调凭据，不能验证）");
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
