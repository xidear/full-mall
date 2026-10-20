package com.fullmall.distribution;

import com.fullmall.distribution.app.CommissionLedgerService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 售后关闭策略：只有订单完成且过窗口才解冻；未完成/未到期都不动。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class CommissionReleaseTest {

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
    CommissionLedgerService ledger;

    @Autowired
    JdbcTemplate jdbc;

    private UUID order(String status, Instant fulfilledAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone,
                                    fulfillment_mode, fulfilled_at)
                VALUES (?, ?, ?, 'CNY', 10000, 0, 0, 10000, 'x', '13900002222', 'SHIP', ?)
                """, id, "REL-" + id.toString().substring(0, 8), status,
                fulfilledAt == null ? null : java.sql.Timestamp.from(fulfilledAt));
        return id;
    }

    private UUID commission(UUID orderId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO distribution_commission (id, order_id, promoter_staff_id, order_amount,
                                                     commission_amount, currency, status, frozen_until)
                VALUES (?, ?, ?, 10000, 500, 'CNY', 'FROZEN', NULL)
                """, id, orderId, UUID.randomUUID());
        return id;
    }

    private String statusOf(UUID id) {
        return jdbc.queryForObject("SELECT status FROM distribution_commission WHERE id = ?", String.class, id);
    }

    @Test
    void releasesOnlyAfterWindowSinceFulfilment() {
        UUID oldOrder = order("FULFILLED", Instant.now().minus(10, ChronoUnit.DAYS));
        UUID freshOrder = order("FULFILLED", Instant.now().minus(1, ChronoUnit.DAYS));
        UUID unpaidOrder = order("PAID", null);

        UUID oldCommission = commission(oldOrder);
        UUID freshCommission = commission(freshOrder);
        UUID unpaidCommission = commission(unpaidOrder);

        int released = ledger.releaseAfterSaleClosed(7, Instant.now());

        assertThat(released).isGreaterThanOrEqualTo(1);
        assertThat(statusOf(oldCommission)).isEqualTo("AVAILABLE");
        assertThat(statusOf(freshCommission)).isEqualTo("FROZEN");   // 未过售后窗口
        assertThat(statusOf(unpaidCommission)).isEqualTo("FROZEN");  // 订单未完成
    }
}
