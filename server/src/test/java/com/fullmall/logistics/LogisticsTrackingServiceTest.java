package com.fullmall.logistics;

import com.fullmall.logistics.app.LogisticsTrackingService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 物流轨迹：演示用 mock；生产配置缺失时**内部告警 + 顾客通用提示**。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.logistics.api-key="
        })
@ActiveProfiles("test")
class LogisticsTrackingServiceTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
        } catch (Exception error) {
            throw new IllegalStateException("无法启动嵌入式 PostgreSQL", error);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    LogisticsTrackingService tracking;

    @Autowired
    JdbcTemplate jdbc;

    private UUID seedShippedOrder() {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'FULFILLED', 'CNY', 1000, 0, 0, 1000, '物流测试', '13911112222', 'SHIP')
                """, orderId, "TRK-" + orderId);
        jdbc.update("""
                INSERT INTO fulfillment_order (id, order_id, type, status, carrier_code, tracking_no)
                VALUES (?, ?, 'WAREHOUSE_SHIP', 'SHIPPED', 'SF', ?)
                """, UUID.randomUUID(), orderId, "SF" + orderId.toString().substring(0, 8));
        return orderId;
    }

    private void setProvider(String value) {
        jdbc.update("""
                INSERT INTO system_setting (setting_key, setting_value) VALUES ('logistics_provider', ?)
                ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value
                """, value);
    }

    @Test
    void mockProviderReturnsEvents() {
        setProvider("mock");
        UUID orderId = seedShippedOrder();

        var events = tracking.trackOrder(orderId);

        assertThat(events).isNotEmpty();
        assertThat(events.get(0).status()).isNotBlank();
    }

    @Test
    void missingProductionConfigAlertsInternallyAndHidesDetailsFromCustomer() {
        setProvider("kuaidi100");
        jdbc.update("DELETE FROM risk_alert WHERE rule_code = 'LOGISTICS_PROVIDER_UNAVAILABLE'");
        UUID orderId = seedShippedOrder();

        assertThatThrownBy(() -> tracking.trackOrder(orderId))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("暂时无法查询");

        // 内部告警必须存在，且带具体原因
        var details = jdbc.queryForList(
                "SELECT detail FROM risk_alert WHERE rule_code = 'LOGISTICS_PROVIDER_UNAVAILABLE'",
                String.class);
        assertThat(details).isNotEmpty();
        assertThat(details.get(0)).contains("api-key");

        setProvider("mock");
    }
}
