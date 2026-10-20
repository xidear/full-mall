package com.fullmall.order;

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

/** 排序白名单：允许的字段生效，未知字段回落默认，**不接受任意 SQL 片段**。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class OrderSortTest {

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
    com.fullmall.order.app.OrderService orders;

    @Autowired
    JdbcTemplate jdbc;

    private void seed(String no, long total) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', ?, 0, 0, ?, 'sort', '13900001111', 'SHIP')
                """, id, "SORT-" + no, total, total);
    }

    @Test
    void sortsByAmountAndFallsBackSafelyOnUnknownField() {
        seed("A", 1000);
        seed("B", 9000);

        var desc = orders.listOrders("PAID", 0, 10, "totalAmount", "desc");
        var asc = orders.listOrders("PAID", 0, 10, "totalAmount", "asc");

        assertThat(firstTotal(desc)).isGreaterThanOrEqualTo(firstTotal(asc));

        // 未知字段 + 恶意片段都必须安全回落，不抛错
        var malicious = orders.listOrders("PAID", 0, 10, "total_amount; DROP TABLE orders", "desc");
        assertThat(malicious).containsKey("items");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isPositive();
    }

    private long firstTotal(java.util.Map<String, Object> page) {
        var items = (java.util.List<?>) page.get("items");
        var summary = (com.fullmall.order.api.OrderSummary) items.get(0);
        return summary.total().amount();
    }
}
