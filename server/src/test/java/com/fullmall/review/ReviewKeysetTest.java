package com.fullmall.review;

import com.fullmall.review.app.ProductReviewService;
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

/** 后台评价 keyset：两页不重不漏。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ReviewKeysetTest {

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
    ProductReviewService reviews;

    @Autowired
    JdbcTemplate jdbc;

    private void review(int index) {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, 'RK', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1000, 'CNY')
                """, skuId, productId, "RK-" + index + "-" + skuId.toString().substring(0, 4));
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance,
                                    stored_value_balance, currency)
                VALUES (?, ?, 'u', ?, 'STANDARD', 0, 0, 'CNY')
                """, memberId, "M-RK-" + index, "1370000000" + index);
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone,
                                    fulfillment_mode, member_id)
                VALUES (?, ?, 'FULFILLED', 'CNY', 1000, 0, 0, 1000, 'p', '13800000000', 'SHIP', ?)
                """, orderId, "RK-ORD-" + index, memberId);
        jdbc.update("""
                INSERT INTO product_review (id, sku_id, member_id, order_id, rating, content, status, created_at)
                VALUES (?, ?, ?, ?, 5, 'ok', 'PUBLISHED', now() - (? || ' minutes')::interval)
                """, UUID.randomUUID(), skuId, memberId, orderId, String.valueOf(index));
    }

    @Test
    void twoPagesDoNotOverlap() {
        for (int i = 1; i <= 5; i++) {
            review(i);
        }

        Map<String, Object> first = reviews.listForAdminKeyset("PUBLISHED", null, null, 2);
        var firstItems = (List<ProductReviewService.AdminReview>) first.get("items");
        Map<String, Object> second = reviews.listForAdminKeyset("PUBLISHED",
                (String) first.get("nextCreatedAt"), (UUID) first.get("nextId"), 10);
        var secondItems = (List<ProductReviewService.AdminReview>) second.get("items");

        assertThat(firstItems).hasSize(2);
        assertThat(firstItems.stream().map(ProductReviewService.AdminReview::id).toList())
                .doesNotContainAnyElementsOf(secondItems.stream()
                        .map(ProductReviewService.AdminReview::id).toList());
        assertThat(firstItems.size() + secondItems.size()).isEqualTo(5);
    }
}
