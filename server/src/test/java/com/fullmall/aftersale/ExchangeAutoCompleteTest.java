package com.fullmall.aftersale;

import com.fullmall.aftersale.app.ExchangeAutoCompleteTask;
import com.fullmall.aftersale.infra.AfterSaleRepository;
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

/** 换货到期自动完成：无争议→完成；顾客报未收到→永不自动完成。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.after-sale.exchange-auto-complete-days=7"
        })
@ActiveProfiles("test")
class ExchangeAutoCompleteTest {

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
    ExchangeAutoCompleteTask task;

    @Autowired
    AfterSaleRepository repository;

    @Autowired
    JdbcTemplate jdbc;

    private UUID exchangeDue(boolean disputed) {
        UUID orderId = UUID.randomUUID();
        UUID afterSaleId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'FULFILLED', 'CNY', 1000, 0, 0, 1000, 'p', '13800000000', 'SHIP')
                """, orderId, "EX-" + orderId.toString().substring(0, 8));
        jdbc.update("""
                INSERT INTO after_sale (id, after_sale_no, order_id, type, status, reason, contact_phone,
                                        refund_amount, currency, exchange_shipped_at, auto_complete_at,
                                        disputed_at)
                VALUES (?, ?, ?, 'EXCHANGE', 'EXCHANGE_SHIPPED', '换货', '13800000000', 0, 'CNY',
                        now() - interval '8 days', now() - interval '1 day', ?)
                """, afterSaleId, "AS-" + afterSaleId.toString().substring(0, 8), orderId,
                disputed ? java.sql.Timestamp.from(java.time.Instant.now()) : null);
        return afterSaleId;
    }

    private String statusOf(UUID id) {
        return jdbc.queryForObject("SELECT status FROM after_sale WHERE id = ?", String.class, id);
    }

    @Test
    void dueExchangeIsAutoCompleted() {
        UUID id = exchangeDue(false);

        task.run();

        assertThat(statusOf(id)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT auto_completed FROM after_sale WHERE id = ?",
                Boolean.class, id)).isTrue();
    }

    @Test
    void disputedExchangeIsNeverAutoCompleted() {
        UUID id = exchangeDue(true);

        task.run();

        assertThat(statusOf(id)).isEqualTo("EXCHANGE_SHIPPED");
        assertThat(repository.findAutoCompletable(10)).doesNotContain(id);
    }

    @Test
    void defaultDaysAreSane() {
        assertThat(task.autoCompleteDays()).isEqualTo(7);
    }
}
