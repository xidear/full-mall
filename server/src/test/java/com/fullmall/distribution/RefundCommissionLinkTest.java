package com.fullmall.distribution;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 退款 → 佣金联动（真实 PostgreSQL）。
 *
 * <p>整单退款必须回冲佣金；部分退款必须延长冻结而**不能**提前解冻。
 * 这两条是资金正确性路径，用集成测试验证，不用 mock 校验 SQL。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.payment.provider=mock",
                "app.demo.read-only=false"
        })
@ActiveProfiles("test")
class RefundCommissionLinkTest {

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
    com.fullmall.payment.app.PaymentService paymentService;

    @Autowired
    JdbcTemplate jdbc;

    private UUID orderId;
    private UUID commissionId;

    @BeforeEach
    void seed() {
        orderId = UUID.randomUUID();
        commissionId = UUID.randomUUID();

        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 10000, 0, 0, 10000, '测试', '13800000000', 'SHIP')
                """, orderId, "T-" + orderId);
        // 真实订单必有商品行：退款要按行分摊
        jdbc.update("""
                INSERT INTO order_line (id, order_id, sku_id, title_snapshot, sku_code_snapshot, quantity,
                                        unit_price_amount, currency, fulfillment_type, line_type)
                VALUES (?, ?, ?, '测试商品', 'T-SKU', 1, 10000, 'CNY', 'WAREHOUSE_SHIP', 'NORMAL')
                """, UUID.randomUUID(), orderId, UUID.randomUUID());
        jdbc.update("""
                INSERT INTO payment (id, order_id, provider, status, amount_amount, amount_currency)
                VALUES (?, ?, 'MOCK', 'PAID', 10000, 'CNY')
                """, UUID.randomUUID(), orderId);
        jdbc.update("""
                INSERT INTO distribution_commission (id, order_id, promoter_staff_id, order_amount,
                                                     commission_amount, currency, status, frozen_until)
                VALUES (?, ?, ?, 10000, 500, 'CNY', 'FROZEN', now() + interval '7 days')
                """, commissionId, orderId, UUID.randomUUID());
    }

    private String commissionStatus() {
        return jdbc.queryForObject(
                "SELECT status FROM distribution_commission WHERE id = ?", String.class, commissionId);
    }

    @Test
    void fullRefundReversesCommissionAndWritesLedger() {
        paymentService.refund(orderId, 10000);

        assertThat(commissionStatus()).isEqualTo("REVERSED");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM commission_ledger WHERE commission_id = ? AND to_status = 'REVERSED'",
                Integer.class, commissionId)).isEqualTo(1);
    }

    @Test
    void partialRefundKeepsCommissionFrozenAndExtendsIt() {
        var before = jdbc.queryForObject(
                "SELECT frozen_until FROM distribution_commission WHERE id = ?",
                java.sql.Timestamp.class, commissionId).toInstant();

        paymentService.refund(orderId, 4000);

        assertThat(commissionStatus()).isEqualTo("FROZEN");
        var after = jdbc.queryForObject(
                "SELECT frozen_until FROM distribution_commission WHERE id = ?",
                java.sql.Timestamp.class, commissionId).toInstant();
        assertThat(after).isAfter(before);
    }

    @Test
    void refundRecordsLineAllocationWithoutDoubleCounting() {
        // 直接退款（非售后单）：应落行分摊，且合计等于退款金额
        paymentService.refund(orderId, 4_000);

        var sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount_amount), 0) FROM order_line_refund WHERE order_id = ?",
                Long.class, orderId);
        assertThat(sum).isEqualTo(4_000);
    }

    @Test
    void refundWritesAuditEvent() {
        paymentService.refund(orderId, 1_000);

        var events = jdbc.queryForList(
                "SELECT actor, detail FROM audit_event WHERE event_type = 'PAYMENT_REFUNDED'"
                        + " AND resource_id = ?", orderId.toString());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).get("actor")).isEqualTo("system:refund");
        assertThat(events.get(0).get("detail").toString()).contains("1000");
    }
}
