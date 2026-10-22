package com.fullmall.customer;

import com.fullmall.customer.app.CustomerService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 客户 keyset：按手机号聚合后分页，两页不重不漏。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class CustomerKeysetTest {

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
    CustomerService customers;

    @Autowired
    JdbcTemplate jdbc;

    private void order(String phone, int minutesAgo) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone,
                                    fulfillment_mode, created_at)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, 'p', ?, 'SHIP',
                        now() - (? || ' minutes')::interval)
                """, id, "CK-" + id.toString().substring(0, 8), phone, String.valueOf(minutesAgo));
    }

    @Test
    @SuppressWarnings("unchecked")
    void twoPagesDoNotOverlap() {
        order("13900000001", 1);
        order("13900000002", 2);
        order("13900000003", 3);
        order("13900000004", 4);
        order("13900000005", 5);

        Map<String, Object> first = customers.listKeyset(null, null, 2);
        var firstItems = (List<com.fullmall.customer.api.CustomerSummary>) first.get("items");
        Map<String, Object> second = customers.listKeyset(
                (String) first.get("nextLastOrderAt"), (String) first.get("nextPhone"), 10);
        var secondItems = (List<com.fullmall.customer.api.CustomerSummary>) second.get("items");

        assertThat(firstItems).hasSize(2);
        assertThat(firstItems.stream().map(com.fullmall.customer.api.CustomerSummary::phone).toList())
                .doesNotContainAnyElementsOf(secondItems.stream()
                        .map(com.fullmall.customer.api.CustomerSummary::phone).toList());
        assertThat(firstItems.size() + secondItems.size()).isEqualTo(5);
    }
}
