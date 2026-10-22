package com.fullmall.supplier;

import com.fullmall.supplier.app.SupplierReturnService;
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

/** 供应商退货对账：只统计 ACCEPTED，按供应商汇总。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ReturnReconciliationTest {

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
    SupplierReturnService returns;

    @Autowired
    JdbcTemplate jdbc;

    private UUID supplier(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO supplier (id, code, name, active)
                VALUES (?, ?, ?, true)
                """, id, "SUP-" + id.toString().substring(0, 8), name);
        return id;
    }

    private void returnRow(UUID supplierId, String status, long amount) {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, 'p', '13800000000', 'SHIP')
                """, orderId, "SR-" + orderId.toString().substring(0, 8));
        jdbc.update("""
                INSERT INTO supplier_return (id, supplier_id, order_id, reason, amount_amount, currency,
                                             status, created_by)
                VALUES (?, ?, ?, '对账测试', ?, 'CNY', ?, 'admin')
                """, UUID.randomUUID(), supplierId, orderId, amount, status);
    }

    @Test
    void onlyAcceptedReturnsAreAggregatedPerSupplier() {
        UUID supplierId = supplier("对账供应商");
        returnRow(supplierId, "ACCEPTED", 1_500);
        returnRow(supplierId, "ACCEPTED", 2_500);
        returnRow(supplierId, "REQUESTED", 9_999);

        var rows = returns.reconciliation(30);

        assertThat(rows).anySatisfy(row -> {
            assertThat(row.get("supplierId")).isEqualTo(supplierId);
            assertThat(((Number) row.get("acceptedCount")).intValue()).isEqualTo(2);
            assertThat(((Number) row.get("acceptedAmount")).longValue()).isEqualTo(4_000);
        });
        assertThat(rows).allSatisfy(row ->
                assertThat(((Number) row.get("acceptedAmount")).longValue()).isNotEqualTo(9_999));
    }

    /** 对账审计：未生成结算单时差额 = 已同意金额（必须告警，不能静默为 0）。 */
    @Test
    void auditReportsDifferenceWhenSettlementMissing() {
        UUID supplierId = supplier("差额供应商");
        returnRow(supplierId, "ACCEPTED", 3_000);

        var audit = returns.reconciliationAudit(30);

        assertThat(((Number) audit.get("acceptedReturnAmount")).longValue()).isGreaterThanOrEqualTo(3_000);
        assertThat(audit.get("consistent")).isEqualTo(false);
        assertThat(((Number) audit.get("difference")).longValue())
                .isEqualTo(((Number) audit.get("acceptedReturnAmount")).longValue()
                        - ((Number) audit.get("settledDeductionAmount")).longValue());
    }

    /** 待办计数只统计 REQUESTED（已处理的不再打扰供应商）。 */
    @Test
    void pendingCountOnlyCountsRequested() {
        UUID supplierId = supplier("待办供应商");
        returnRow(supplierId, "REQUESTED", 100);
        returnRow(supplierId, "ACCEPTED", 200);
        returnRow(supplierId, "SETTLED", 300);

        assertThat(returns.pendingCount(supplierId)).isEqualTo(1);
    }
}
