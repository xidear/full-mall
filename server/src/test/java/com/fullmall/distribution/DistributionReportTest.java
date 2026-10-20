package com.fullmall.distribution;

import com.fullmall.distribution.app.DistributionReportService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 报表口径：冻结/可提现（已扣消耗）/已提现、工资与钱包分离。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class DistributionReportTest {

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
    DistributionReportService reports;

    @Autowired
    JdbcTemplate jdbc;

    private UUID order() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 10000, 0, 0, 10000, 'r', '13900003333', 'SHIP')
                """, id, "RPT-" + id.toString().substring(0, 8));
        return id;
    }

    private void commission(UUID orderId, UUID promoter, String status, String channel,
                            long amount, long consumed) {
        jdbc.update("""
                INSERT INTO distribution_commission (id, order_id, promoter_staff_id, order_amount,
                                                     commission_amount, currency, status, frozen_until,
                                                     level, promoter_type, payee_channel, consumed_amount)
                VALUES (?, ?, ?, 10000, ?, 'CNY', ?, NULL, 1, 'STAFF', ?, ?)
                """, UUID.randomUUID(), orderId, promoter, amount, status, channel, consumed);
    }

    @Test
    void totalsSplitByStatusAndChannel() {
        UUID promoter = UUID.randomUUID();
        commission(order(), promoter, "FROZEN", "PAYROLL", 500, 0);
        commission(order(), promoter, "AVAILABLE", "WALLET", 300, 100);   // 已消耗 100
        commission(order(), promoter, "WITHDRAWN", "WALLET", 200, 200);

        var report = reports.report(LocalDate.now(), LocalDate.now());

        var row = report.rows().stream().filter(r -> r.promoterId().equals(promoter)).findFirst()
                .orElseThrow();
        assertThat(row.frozenAmount()).isEqualTo(500);
        assertThat(row.availableAmount()).isEqualTo(200);   // 300 - 100
        assertThat(row.withdrawnAmount()).isEqualTo(200);
        assertThat(row.payrollAmount()).isEqualTo(500);
        assertThat(row.walletAmount()).isEqualTo(500);      // 300 + 200
        assertThat(row.commissionAmount()).isEqualTo(1_000);
        assertThat(report.totals().commissionAmount()).isGreaterThanOrEqualTo(1_000);
    }

    @Test
    void invalidRangeIsRejected() {
        assertThatThrownBy(() -> reports.report(LocalDate.now(), LocalDate.now().minusDays(1)))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("结束日期");
    }
}
