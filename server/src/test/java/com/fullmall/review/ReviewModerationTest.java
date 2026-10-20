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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 评价审核：隐藏后前台不可见、后台可见，可恢复。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ReviewModerationTest {

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

    /** 真实外键：必须先建商品与 SKU，再插评价。 */
    private UUID sku() {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '评价测试商品', 'ACTIVE')",
                productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1000, 'CNY')
                """, skuId, productId, "SKU-RV-" + skuId.toString().substring(0, 8));
        return skuId;
    }

    private UUID member() {
        UUID memberId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance,
                                    stored_value_balance, currency)
                VALUES (?, ?, '评价用户', ?, 'STANDARD', 0, 0, 'CNY')
                """, memberId, "M-RV-" + memberId.toString().substring(0, 8),
                "139" + memberId.toString().replaceAll("[^0-9]", "1").substring(0, 8));
        return memberId;
    }

    private UUID order() {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'FULFILLED', 'CNY', 1000, 0, 0, 1000, '评价用户', '13900007777', 'SHIP')
                """, orderId, "RV-" + orderId.toString().substring(0, 8));
        return orderId;
    }

    private UUID review(UUID skuId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product_review (id, order_id, sku_id, member_id, rating, content, status)
                VALUES (?, ?, ?, ?, 5, '很好', 'PUBLISHED')
                """, id, order(), skuId, member());
        return id;
    }

    @Test
    void hiddenReviewDisappearsFromStorefrontButStaysInAdmin() {
        UUID skuId = sku();
        UUID reviewId = review(skuId);

        assertThat(reviews.listForSku(skuId, 10)).hasSize(1);

        reviews.moderate(reviewId, "HIDDEN");

        assertThat(reviews.listForSku(skuId, 10)).isEmpty();
        assertThat(reviews.listForAdmin("HIDDEN", 0, 10))
                .anySatisfy(item -> assertThat(item.id()).isEqualTo(reviewId));

        reviews.moderate(reviewId, "published");   // 大小写不敏感地恢复
        assertThat(reviews.listForSku(skuId, 10)).hasSize(1);
    }

    @Test
    void invalidStatusIsRejected() {
        UUID reviewId = review(sku());

        assertThatThrownBy(() -> reviews.moderate(reviewId, "DELETED"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("PUBLISHED 或 HIDDEN");
        assertThatThrownBy(() -> reviews.moderate(UUID.randomUUID(), "HIDDEN"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("评价不存在");
    }
}
