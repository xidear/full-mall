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

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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

    /** 快递100 官方报文的桩服务（等价于官方 mock）：只验证我们的请求与解析，不发外网。 */
    private static final AtomicReference<String> LAST_FORM = new AtomicReference<>();
    private static final com.sun.net.httpserver.HttpServer KUAIDI100_STUB;

    static {
        try {
            KUAIDI100_STUB = com.sun.net.httpserver.HttpServer.create(
                    new InetSocketAddress("127.0.0.1", 0), 0);
            KUAIDI100_STUB.createContext("/poll/query.do", exchange -> {
                LAST_FORM.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] body = """
                        {"message":"ok","status":"200","data":[
                          {"time":"2026-10-20 10:00:00","context":"已揽收","areaName":"上海"},
                          {"time":"2026-10-21 08:30:00","context":"运输中","areaName":"杭州"}]}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            KUAIDI100_STUB.start();
        } catch (Exception error) {
            throw new IllegalStateException("无法启动快递100 桩服务", error);
        }
    }

    @DynamicPropertySource
    static void kuaidi100Endpoint(DynamicPropertyRegistry registry) {
        registry.add("app.logistics.kuaidi100.endpoint",
                () -> "http://127.0.0.1:" + KUAIDI100_STUB.getAddress().getPort() + "/poll/query.do");
    }

    @Autowired
    LogisticsTrackingService tracking;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    com.fullmall.setting.app.SystemSettingService settings;

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
    void adminConfiguredKeyIsUsedAndMasked() {
        setProvider("kuaidi100");
        jdbc.update("""
                INSERT INTO system_setting (setting_key, setting_value) VALUES ('logistics_key', 'demo-key-123')
                ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value
                """);

        // 后台读取只能看到掩码，不能回传明文
        var admin = settings.adminSettings();
        assertThat(admin.get("logistics_key")).isEqualTo("******");
        // 内部取值才是明文
        assertThat(settings.rawValue("logistics_key")).isEqualTo("demo-key-123");

        setProvider("mock");
    }

    @Test
    void missingProductionConfigAlertsInternallyAndHidesDetailsFromCustomer() {
        setProvider("kuaidi100");
        // 用例间共享库：清掉上一条用例可能写入的 AK / customer，保证"未配置"前提
        jdbc.update("DELETE FROM system_setting WHERE setting_key IN ('logistics_key','logistics_customer')");
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
        // 未配置 AK 时应明确提示"未配置物流服务商 AK"（内部可见，顾客不可见）
        assertThat(details.get(0)).contains("AK");

        setProvider("mock");
    }

    @Test
    void kuaidi100ProviderReturnsParsedTrajectoryWithConfiguredCredentials() {
        setProvider("kuaidi100");
        jdbc.update("""
                INSERT INTO system_setting (setting_key, setting_value) VALUES ('logistics_key', 'demo-key-123')
                ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value
                """);
        jdbc.update("""
                INSERT INTO system_setting (setting_key, setting_value) VALUES ('logistics_customer', 'cust-100')
                ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value
                """);
        UUID orderId = seedShippedOrder();

        var events = tracking.trackOrder(orderId);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).status()).isEqualTo("已揽收");
        assertThat(events.get(1).location()).isEqualTo("杭州");
        // 请求必须带后台配置的 customer 与签名，且密钥不得出现在请求体里
        String form = LAST_FORM.get();
        assertThat(form).contains("customer=cust-100");
        assertThat(form).contains("sign=");
        assertThat(form).doesNotContain("demo-key-123");

        setProvider("mock");
    }
}
