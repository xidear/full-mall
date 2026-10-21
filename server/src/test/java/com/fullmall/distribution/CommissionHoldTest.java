package com.fullmall.distribution;

import com.fullmall.distribution.app.CommissionHoldService;
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

/** 部分售后冻结佣金：退货完成则回冲；驳回/换货则解冻不扣。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class CommissionHoldTest {

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
    CommissionHoldService holds;

    @Autowired
    JdbcTemplate jdbc;

    private UUID order(long total) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', ?, 0, 0, ?, 'p', '13900001111', 'SHIP')
                """, id, "CH-" + id.toString().substring(0, 8), total, total);
        return id;
    }

    private UUID commission(UUID orderId, long amount) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO distribution_commission (id, order_id, promoter_staff_id, order_amount,
                                                     commission_amount, currency, status, payee_channel)
                VALUES (?, ?, ?, 10000, ?, 'CNY', 'AVAILABLE', 'WALLET')
                """, id, orderId, UUID.randomUUID(), amount);
        return id;
    }

    @Test
    void refundClosesWithReversalOtherwiseReleasesHold() {
        // 退货完成：1000 佣金、订单 10000、退 4000 → 冻结 400，关闭时回冲 400
        UUID refundOrder = order(10_000);
        UUID refundCommission = commission(refundOrder, 1_000);
        UUID refundAfterSale = UUID.randomUUID();
        holds.hold(refundOrder, refundAfterSale, 4_000, 10_000);

        assertThat(holdAmount(refundCommission)).isEqualTo(400);
        holds.close(refundAfterSale, true);

        var row = jdbc.queryForMap("""
                SELECT reversed_amount, hold_after_sale_id FROM distribution_commission WHERE id = ?
                """, refundCommission);
        assertThat(((Number) row.get("reversed_amount")).longValue()).isEqualTo(400);
        assertThat(row.get("hold_after_sale_id")).isNull();

        // 驳回：冻结解除、金额不变
        UUID rejectOrder = order(10_000);
        UUID rejectCommission = commission(rejectOrder, 1_000);
        UUID rejectAfterSale = UUID.randomUUID();
        holds.hold(rejectOrder, rejectAfterSale, 2_000, 10_000);
        holds.close(rejectAfterSale, false);

        var rejected = jdbc.queryForMap("""
                SELECT reversed_amount, hold_amount, hold_after_sale_id
                  FROM distribution_commission WHERE id = ?
                """, rejectCommission);
        assertThat(((Number) rejected.get("reversed_amount")).longValue()).isZero();
        assertThat(((Number) rejected.get("hold_amount")).longValue()).isZero();
        assertThat(rejected.get("hold_after_sale_id")).isNull();
    }

    private long holdAmount(UUID commissionId) {
        Long value = jdbc.queryForObject(
                "SELECT hold_amount FROM distribution_commission WHERE id = ?", Long.class, commissionId);
        return value == null ? 0 : value;
    }
}
