package com.fullmall.payment;

import com.fullmall.payment.reconcile.PaymentReconciliationService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 对账：四类结果都能识别，差异可查，重复导入幂等。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class PaymentReconciliationTest {

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
    PaymentReconciliationService reconciliation;

    @Autowired
    JdbcTemplate jdbc;

    private void order(String orderNo, String status, long amount) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, ?, 'CNY', ?, 0, 0, ?, 'p', '13800000000', 'SHIP')
                """, id, orderNo, status, amount, amount);
    }

    @Test
    void classifiesAllFourKindsAndIsIdempotent() {
        String tag = UUID.randomUUID().toString().substring(0, 6);
        order("MATCH-" + tag, "PAID", 1990);
        order("MISMATCH-" + tag, "PAID", 1990);
        order("LOCALONLY-" + tag, "PAID", 500);
        LocalDate date = LocalDate.of(2026, 10, 20);

        var summary = reconciliation.reconcile(date, List.of(
                new PaymentReconciliationService.Row("MATCH-" + tag, 1990),
                new PaymentReconciliationService.Row("MISMATCH-" + tag, 100),
                new PaymentReconciliationService.Row("BILLONLY-" + tag, 800)));

        assertThat(summary.matched()).isEqualTo(1);
        assertThat(summary.amountMismatch()).isEqualTo(1);
        assertThat(summary.billOnly()).isEqualTo(1);
        assertThat(summary.localOnly()).isGreaterThanOrEqualTo(1);

        var diffs = reconciliation.differences(date);
        assertThat(diffs).anySatisfy(row -> assertThat(row).containsEntry("kind", "BILL_ONLY"));
        assertThat(diffs).anySatisfy(row -> assertThat(row).containsEntry("kind", "AMOUNT_MISMATCH"));
        assertThat(diffs).noneSatisfy(row -> assertThat(row).containsEntry("kind", "MATCHED"));

        // 幂等：再导一次同一天，不报唯一冲突
        var again = reconciliation.reconcile(date, List.of(
                new PaymentReconciliationService.Row("MATCH-" + tag, 1990)));
        assertThat(again.matched()).isEqualTo(1);
    }

    @Test
    void rejectsEmptyAndDuplicateBillRows() {
        LocalDate date = LocalDate.of(2026, 10, 21);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                reconciliation.reconcile(date, List.of()))
                .hasMessageContaining("账单没有数据行");

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                reconciliation.reconcile(date, List.of(
                        new PaymentReconciliationService.Row("DUP-1", 100),
                        new PaymentReconciliationService.Row("DUP-1", 100))))
                .hasMessageContaining("商户单号重复");
    }
}
