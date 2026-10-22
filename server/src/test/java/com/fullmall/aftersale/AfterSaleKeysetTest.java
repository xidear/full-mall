package com.fullmall.aftersale;

import com.fullmall.aftersale.app.AfterSaleService;
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

/** 售后 keyset 分页：两页不重不漏。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class AfterSaleKeysetTest {

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
    AfterSaleService afterSales;

    @Autowired
    JdbcTemplate jdbc;

    private void afterSale(int index) {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'FULFILLED', 'CNY', 1000, 0, 0, 1000, 'p', '13800000000', 'SHIP')
                """, orderId, "AK-" + index);
        jdbc.update("""
                INSERT INTO after_sale (id, after_sale_no, order_id, type, status, reason,
                                        contact_phone, refund_amount, currency, created_at)
                VALUES (?, ?, ?, 'RETURN', 'APPROVED', '测试', '13800000000', 1000, 'CNY',
                        now() - (? || ' minutes')::interval)
                """, UUID.randomUUID(), "AS-K-" + index, orderId, String.valueOf(index));
    }

    @Test
    void twoPagesDoNotOverlap() {
        for (int i = 1; i <= 5; i++) {
            afterSale(i);
        }

        Map<String, Object> first = afterSales.listKeyset("APPROVED", null, null, 2);
        var firstIds = ((List<com.fullmall.aftersale.api.AfterSaleDtos.AfterSaleView>) first.get("items"))
                .stream().map(com.fullmall.aftersale.api.AfterSaleDtos.AfterSaleView::id).toList();
        var second = afterSales.listKeyset("APPROVED",
                (String) first.get("nextCreatedAt"), (UUID) first.get("nextId"), 10);
        var ids = ((List<com.fullmall.aftersale.api.AfterSaleDtos.AfterSaleView>) second.get("items"))
                .stream().map(com.fullmall.aftersale.api.AfterSaleDtos.AfterSaleView::id).toList();

        assertThat(firstIds).hasSize(2);
        assertThat(firstIds).doesNotContainAnyElementsOf(ids);
        assertThat(firstIds.size() + ids.size()).isEqualTo(5);
    }
}
