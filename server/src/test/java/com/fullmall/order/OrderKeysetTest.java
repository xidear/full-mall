package com.fullmall.order;

import com.fullmall.order.app.OrderService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Keyset 分页：两页衔接不重不漏，游标可用。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class OrderKeysetTest {

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
    OrderService orders;

    @Autowired
    JdbcTemplate jdbc;

    private void order(int index) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone,
                                    fulfillment_mode, created_at)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, 'p', '13800000000', 'SHIP',
                        now() - (? || ' minutes')::interval)
                """, id, "KS-" + index, String.valueOf(index));
    }

    @Test
    void twoPagesDoNotOverlapOrMiss() {
        for (int i = 1; i <= 5; i++) {
            order(i);
        }

        Map<String, Object> first = orders.listOrdersKeyset("PAID", null, null, 2);
        assertThat((java.util.List<?>) first.get("items")).hasSize(2);

        String cursorAt = (String) first.get("nextCreatedAt");
        UUID cursorId = (UUID) first.get("nextId");
        Map<String, Object> second = orders.listOrdersKeyset("PAID", cursorAt, cursorId, 10);

        var firstIds = ((java.util.List<com.fullmall.order.api.OrderSummary>) first.get("items"))
                .stream().map(com.fullmall.order.api.OrderSummary::id).toList();
        var secondIds = ((java.util.List<com.fullmall.order.api.OrderSummary>) second.get("items"))
                .stream().map(com.fullmall.order.api.OrderSummary::id).toList();

        assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
        assertThat(firstIds.size() + secondIds.size()).isEqualTo(5);
    }
}
