package com.fullmall.payment;

import com.fullmall.payment.app.PaymentService;
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

/** 累计退款不得超付：单次校验拦不住"两次加起来超了"。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.payment.provider=mock"
        })
@ActiveProfiles("test")
class CumulativeRefundTest {

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
    PaymentService payments;

    @Autowired
    JdbcTemplate jdbc;

    private UUID paidOrder(long amount) {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', ?, 0, 0, ?, 'p', '13900001234', 'SHIP')
                """, orderId, "REF-" + orderId.toString().substring(0, 8), amount, amount);
        jdbc.update("""
                INSERT INTO order_line (id, order_id, sku_id, title_snapshot, sku_code_snapshot, quantity,
                                        unit_price_amount, currency, fulfillment_type, line_type)
                VALUES (?, ?, ?, '商品', 'SKU', 2, ?, 'CNY', 'WAREHOUSE_SHIP', 'NORMAL')
                """, UUID.randomUUID(), orderId, UUID.randomUUID(), amount / 2);
        jdbc.update("""
                INSERT INTO payment (id, order_id, provider, status, amount_amount, amount_currency,
                                     refunded_amount)
                VALUES (?, ?, 'MOCK', 'PAID', ?, 'CNY', 0)
                """, paymentId, orderId, amount);
        return orderId;
    }

    private long refunded(UUID orderId) {
        Long value = jdbc.queryForObject(
                "SELECT refunded_amount FROM payment WHERE order_id = ?", Long.class, orderId);
        return value == null ? 0 : value;
    }

    @Test
    void rejectsRefundExceedingRemainingAfterPartialRefund() {
        UUID orderId = paidOrder(10_000);

        payments.refund(orderId, 4_000);

        assertThat(refunded(orderId)).isEqualTo(4_000);

        // 再退 8_000 会累计 12_000 > 10_000，必须拒绝
        assertThatThrownBy(() -> payments.refund(orderId, 8_000))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("退款超过可退金额");

        // 退剩余 6_000 成功
        payments.refund(orderId, 6_000);
        assertThat(refunded(orderId)).isEqualTo(10_000);
    }
}
