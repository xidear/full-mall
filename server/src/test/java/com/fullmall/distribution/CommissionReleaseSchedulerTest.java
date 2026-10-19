package com.fullmall.distribution;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 解冻调度：冻结期满的佣金应被自动置为 AVAILABLE，并留下流水。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.distribution.release.enabled=true",
                "app.distribution.release.interval-ms=200",
                "app.distribution.release.initial-delay-ms=200"
        })
@ActiveProfiles({"test", "dev"})
class CommissionReleaseSchedulerTest {

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
    JdbcTemplate jdbc;

    @Test
    void expiredFrozenCommissionBecomesAvailable() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID commissionId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, '测试', '13800000000', 'SHIP')
                """, orderId, "REL-" + orderId);
        jdbc.update("""
                INSERT INTO distribution_commission (id, order_id, promoter_staff_id, order_amount,
                                                     commission_amount, currency, status, frozen_until)
                VALUES (?, ?, ?, 1000, 50, 'CNY', 'FROZEN', now() - interval '1 minute')
                """, commissionId, orderId, UUID.randomUUID());

        Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
        String status = "FROZEN";
        while (Instant.now().isBefore(deadline)) {
            status = jdbc.queryForObject(
                    "SELECT status FROM distribution_commission WHERE id = ?", String.class, commissionId);
            if ("AVAILABLE".equals(status)) {
                break;
            }
            Thread.sleep(200);
        }

        assertThat(status).isEqualTo("AVAILABLE");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM commission_ledger WHERE commission_id = ? AND to_status = 'AVAILABLE'",
                Integer.class, commissionId)).isEqualTo(1);
    }
}
