package com.fullmall.notification;

import com.fullmall.order.api.CheckoutRequest;
import com.fullmall.order.app.OrderService;
import com.fullmall.order.domain.FulfillmentMode;
import com.fullmall.payment.app.PaymentService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.payment.provider=mock"
        })
@ActiveProfiles("test")
class OrderNotificationFlowTest {

    private static final EmbeddedPostgres POSTGRES;
    private UUID memberId;
    private String phone;
    private static final UUID PRODUCT = UUID.fromString("dddddddd-0000-0000-0000-0000000000bb");
    private static final UUID SKU = UUID.fromString("dddddddd-0000-0000-0000-0000000000cc");
    private static final UUID WAREHOUSE = UUID.fromString("dddddddd-0000-0000-0000-0000000000ee");

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
    OrderService orders;

    @Autowired
    PaymentService payments;

    @Autowired
    JdbcTemplate jdbc;

    private UUID batchId;

    private void seed() {
        jdbc.update("DELETE FROM notification");
        // 批次 id 改为每条用例随机：固定 id 会与 stock_move / reservation 的外键打架
        batchId = java.util.UUID.randomUUID();
        memberId = java.util.UUID.randomUUID();
        phone = "139" + String.format("%08d", Math.abs(memberId.hashCode() % 100_000_000));

        jdbc.update("""
                INSERT INTO market (code, name, currency, default_locale, active)
                VALUES ('CN-NOTI', '通知测试市场', 'CNY', 'zh-CN', TRUE) ON CONFLICT (code) DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO warehouse (id, market_code, name, type, active)
                VALUES (?, 'CN-NOTI', '通知测试仓', 'SELF', TRUE) ON CONFLICT (id) DO NOTHING
                """, WAREHOUSE);
        jdbc.update("""
                INSERT INTO product (id, title, status) VALUES (?, '通知测试商品', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
                """, PRODUCT);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, 'SKU-NOTI', 1000, 'CNY') ON CONFLICT (id) DO NOTHING
                """, SKU, PRODUCT);
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no,
                                            quantity, reserved_quantity, status)
                VALUES (?, ?, 'WAREHOUSE', ?, ?, 50, 0, 'AVAILABLE')
                """, batchId, SKU, WAREHOUSE, "NOTI-" + batchId);
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, ?, '通知用户', ?, 'GOLD', 0, 0, 'CNY')
                """, memberId, "M-NOTI-" + memberId.toString().substring(0, 8), phone);
    }

    @Test
    void cancelWritesMemberNotification() {
        seed();
        var order = orders.checkout(new CheckoutRequest(
                List.of(new CheckoutRequest.Item(SKU, 1)),
                FulfillmentMode.SHIP,
                null,
                new CheckoutRequest.Contact("通知用户", phone, null),
                new CheckoutRequest.Address("上海", "上海", "静安", "南京西路 9 号", "200040"),
                List.of(), null, null, null, null, null), null);

        boolean cancelled = orders.cancelUnpaidOrder(order.id());

        assertThat(cancelled).isTrue();
        // 身份统一：站内消息写 MEMBER，短信写 CUSTOMER/手机号
        assertThat(jdbc.queryForList("""
                SELECT channel FROM notification
                 WHERE recipient_type = 'MEMBER' AND recipient_id = ? AND event_type = 'ORDER_CANCELLED'
                """, String.class, memberId.toString())).containsExactly("IN_APP");
        assertThat(jdbc.queryForList("""
                SELECT channel FROM notification
                 WHERE recipient_type = 'CUSTOMER' AND recipient_id = ? AND event_type = 'ORDER_CANCELLED'
                """, String.class, phone)).containsExactly("SMS");
    }

    @Test
    void orderAndPaymentWriteMemberNotifications() {
        seed();

        var order = orders.checkout(new CheckoutRequest(
                List.of(new CheckoutRequest.Item(SKU, 1)),
                FulfillmentMode.SHIP,
                null,
                new CheckoutRequest.Contact("通知用户", phone, null),
                new CheckoutRequest.Address("上海", "上海", "静安", "南京西路 9 号", "200040"),
                List.of(), null, null, null, null, null), null);

        // 下单：恰好一条会员消息，收件人是订单归属会员
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM notification
                 WHERE recipient_type = 'MEMBER' AND recipient_id = ? AND event_type = 'ORDER_CREATED'
                """, Integer.class, memberId.toString())).isEqualTo(1);

        payments.payMock(order.id());

        // 身份统一后：会员订单的站内消息只写到 MEMBER/<会员ID>，短信仍按手机号
        assertThat(jdbc.queryForList("""
                SELECT channel FROM notification
                 WHERE recipient_type = 'CUSTOMER' AND recipient_id = ? AND event_type = 'ORDER_PAID'
                 ORDER BY channel
                """, String.class, phone)).containsExactly("SMS");
        assertThat(jdbc.queryForList("""
                SELECT channel FROM notification
                 WHERE recipient_type = 'MEMBER' AND recipient_id = ? AND event_type = 'ORDER_PAID'
                """, String.class, memberId.toString())).containsExactly("IN_APP");

        // 支付：恰好一条会员消息
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM notification
                 WHERE recipient_type = 'MEMBER' AND recipient_id = ? AND event_type = 'ORDER_PAID'
                """, Integer.class, memberId.toString())).isEqualTo(1);

        // 退款：会员收到一条站内消息，且只一条
        payments.refund(order.id(), order.total().amount());
        assertThat(jdbc.queryForList("""
                SELECT channel FROM notification
                 WHERE recipient_type = 'MEMBER' AND recipient_id = ? AND event_type = 'ORDER_REFUNDED'
                """, String.class, memberId.toString())).containsExactly("IN_APP");
    }
}
