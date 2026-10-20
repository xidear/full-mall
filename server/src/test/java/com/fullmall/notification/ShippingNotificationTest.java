package com.fullmall.notification;

import com.fullmall.fulfillment.app.FulfillmentService;
import com.fullmall.fulfillment.domain.FulfillmentStatus;
import com.fullmall.fulfillment.infra.FulfillmentRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 发货：必须带承运商与运单号，并发通知给顾客。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ShippingNotificationTest {

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
    FulfillmentService fulfillmentService;

    @Autowired
    FulfillmentRepository fulfillmentRepository;

    @Autowired
    JdbcTemplate jdbc;

    private UUID seedOrderWithFulfillment(String phone, FulfillmentStatus status) {
        UUID orderId = UUID.randomUUID();
        String orderNo = "SHIP-" + orderId;
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, '发货测试', ?, 'SHIP')
                """, orderId, orderNo, phone);
        UUID fulfillmentId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO fulfillment_order (id, order_id, type, status)
                VALUES (?, ?, 'WAREHOUSE_SHIP', ?)
                """, fulfillmentId, orderId, status.name());
        return fulfillmentId;
    }

    @Test
    void shippingStoresTrackingAndNotifiesCustomer() {
        String phone = "1390000" + (1000 + Math.abs(UUID.randomUUID().hashCode() % 9000));
        UUID fulfillmentId = seedOrderWithFulfillment(phone, FulfillmentStatus.ACCEPTED);

        var shipped = fulfillmentService.shipFulfillment(fulfillmentId, "SF", "SF123456789");

        assertThat(shipped.status()).isEqualTo(FulfillmentStatus.SHIPPED);
        assertThat(jdbc.queryForObject(
                "SELECT tracking_no FROM fulfillment_order WHERE id = ?", String.class, fulfillmentId))
                .isEqualTo("SF123456789");
        // 游客订单（无 member_id）：站内 + 短信都落在 CUSTOMER/手机号
        assertThat(jdbc.queryForList("""
                SELECT channel FROM notification
                 WHERE recipient_type = 'CUSTOMER' AND recipient_id = ? AND event_type = 'ORDER_SHIPPED'
                 ORDER BY channel
                """, String.class, phone)).containsExactly("IN_APP", "SMS");
    }

    @Test
    void trackingAndCarrierAreRequired() {
        String phone = "1390000" + (1000 + Math.abs(UUID.randomUUID().hashCode() % 9000));
        UUID fulfillmentId = seedOrderWithFulfillment(phone, FulfillmentStatus.ACCEPTED);

        assertThatThrownBy(() -> fulfillmentService.shipFulfillment(fulfillmentId, " ", "X1"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("承运商");
        assertThatThrownBy(() -> fulfillmentService.shipFulfillment(fulfillmentId, "SF", " "))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("运单号");
    }
}
