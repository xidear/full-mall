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

/** 退款原路退回：按支付记录里的渠道退；渠道无适配器时明确拒绝，不回落 Mock。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.payment.provider=mock"
        })
@ActiveProfiles("test")
class RefundOriginalProviderTest {

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

    private UUID paidOrder(String provider) {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 5000, 0, 0, 5000, 'p', '13800000000', 'SHIP')
                """, orderId, "RP-" + orderId.toString().substring(0, 8));
        UUID paymentId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO payment (id, order_id, provider, status, amount_amount, amount_currency,
                                     provider_transaction_id, paid_at)
                VALUES (?, ?, ?, 'PAID', 5000, 'CNY', ?, now())
                """, paymentId, orderId, provider, "txn-" + paymentId.toString().substring(0, 8));
        return orderId;
    }

    @Test
    void mockPaymentRefunds() {
        UUID orderId = paidOrder("MOCK");

        var refunded = payments.refund(orderId, 2000, false);

        assertThat(refunded.status().name()).isIn("REFUNDED", "PARTIALLY_REFUNDED");
    }

    @Test
    void unknownProviderIsRejectedInsteadOfFallingBackToMock() {
        UUID orderId = paidOrder("WECHAT");

        assertThatThrownBy(() -> payments.refund(orderId, 1000, false))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("未配置该渠道的退款适配器")
                .hasMessageContaining("WECHAT");
    }
}
