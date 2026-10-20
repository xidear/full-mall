package com.fullmall.notification;

import com.fullmall.notification.app.NotificationService;
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

/**
 * 通知身份分流规则（**唯一事实来源，防止再次记错**）。
 *
 * <table>
 *   <tr><th>订单</th><th>渠道</th><th>收件人</th></tr>
 *   <tr><td>已绑定会员</td><td>IN_APP</td><td>MEMBER/&lt;memberId&gt;</td></tr>
 *   <tr><td>已绑定会员</td><td>SMS</td><td>CUSTOMER/&lt;手机号&gt;</td></tr>
 *   <tr><td>游客</td><td>任意</td><td>CUSTOMER/&lt;手机号&gt;</td></tr>
 *   <tr><td>任意</td><td>任意</td><td>额外一条 STAFF/ALL</td></tr>
 * </table>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class NotificationRoutingTest {

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
    NotificationService notifications;

    @Autowired
    JdbcTemplate jdbc;

    private UUID order(String phone, UUID memberId) {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone,
                                    fulfillment_mode, member_id)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, '路由测试', ?, 'SHIP', ?)
                """, orderId, "RTE-" + orderId, phone, memberId);
        return orderId;
    }

    private List<String> rows(UUID orderId, String eventType) {
        return jdbc.queryForList("""
                SELECT recipient_type || ':' || recipient_id || ':' || channel
                  FROM notification
                 WHERE event_type = ? AND recipient_id IN (
                       SELECT recipient_id FROM notification WHERE event_type = ?)
                 ORDER BY recipient_type, channel
                """, String.class, eventType, eventType);
    }

    @Test
    void memberOrderRoutesInAppToMemberAndSmsToPhone() {
        String phone = "139" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100_000_000));
        UUID memberId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, ?, '路由会员', ?, 'STANDARD', 0, 0, 'CNY')
                """, memberId, "M-RTE-" + memberId.toString().substring(0, 8), phone);
        UUID orderId = order(phone, memberId);

        notifications.notifyOrderEvent(orderId, "ROUTE_MEMBER", "标题", "内容", List.of("IN_APP", "SMS"));

        var rows = jdbc.query("""
                SELECT recipient_type, recipient_id, channel FROM notification
                 WHERE event_type = 'ROUTE_MEMBER' ORDER BY recipient_type, channel
                """, (rs, i) -> rs.getString("recipient_type") + ":" + rs.getString("recipient_id")
                + ":" + rs.getString("channel"));

        assertThat(rows).contains(
                "MEMBER:" + memberId + ":IN_APP",
                "CUSTOMER:" + phone + ":SMS",
                "STAFF:ALL:IN_APP");
        assertThat(rows).doesNotContain("CUSTOMER:" + phone + ":IN_APP");
    }

    @Test
    void guestOrderRoutesEverythingToPhone() {
        String phone = "138" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100_000_000));
        UUID orderId = order(phone, null);

        notifications.notifyOrderEvent(orderId, "ROUTE_GUEST", "标题", "内容", List.of("IN_APP", "SMS"));

        var rows = jdbc.queryForList("""
                SELECT channel FROM notification
                 WHERE event_type = 'ROUTE_GUEST' AND recipient_type = 'CUSTOMER'
                   AND recipient_id = ? ORDER BY channel
                """, String.class, phone);

        assertThat(rows).containsExactly("IN_APP", "SMS");
    }

    @Test
    void guestOrderGetsCreationNotification() {
        String phone = "137" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100_000_000));
        UUID orderId = order(phone, null);

        notifications.notifyOrderEvent(orderId, "ORDER_CREATED", "订单已创建", "内容",
                List.of("IN_APP", "SMS"));

        assertThat(jdbc.queryForList("""
                SELECT channel FROM notification
                 WHERE event_type = 'ORDER_CREATED' AND recipient_type = 'CUSTOMER'
                   AND recipient_id = ? ORDER BY channel
                """, String.class, phone)).containsExactly("IN_APP", "SMS");
    }
}
