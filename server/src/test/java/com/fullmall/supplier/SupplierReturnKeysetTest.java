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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 供应商退货 keyset：两页不重不漏。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SupplierReturnKeysetTest {

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

    private void ret(int index) {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'FULFILLED', 'CNY', 1000, 0, 0, 1000, 'p', '13800000000', 'SHIP')
                """, orderId, "SRK-" + index);
        jdbc.update("""
                INSERT INTO supplier_return (id, supplier_id, order_id, reason, amount_amount, currency,
                                             status, created_by, created_at)
                VALUES (?, ?, ?, '测试', 1000, 'CNY', 'REQUESTED', 'admin',
                        now() - (? || ' minutes')::interval)
                """, UUID.randomUUID(), UUID.randomUUID(), orderId, String.valueOf(index));
    }

    @Test
    @SuppressWarnings("unchecked")
    void twoPagesDoNotOverlap() {
        for (int i = 1; i <= 5; i++) {
            ret(i);
        }

        Map<String, Object> first = returns.listKeyset(null, "REQUESTED", null, null, 2);
        var firstItems = (List<SupplierReturnService.SupplierReturn>) first.get("items");
        Map<String, Object> second = returns.listKeyset(null, "REQUESTED",
                (String) first.get("nextCreatedAt"),
                (UUID) first.get("nextId"), 10);
        var secondItems = (List<SupplierReturnService.SupplierReturn>) second.get("items");

        assertThat(firstItems).hasSize(2);
        assertThat(firstItems.stream().map(SupplierReturnService.SupplierReturn::id).toList())
                .doesNotContainAnyElementsOf(secondItems.stream()
                        .map(SupplierReturnService.SupplierReturn::id).toList());
        assertThat(firstItems.size() + secondItems.size()).isEqualTo(5);
    }
}
