package com.fullmall.review;

import com.fullmall.review.app.ProductReviewService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ProductReviewServiceTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final UUID SKU = UUID.fromString("eeeeeeee-1111-2222-3333-444444444444");
    private static final UUID PRODUCT = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");
    private static final UUID MEMBER = UUID.fromString("eeeeeeee-9999-9999-9999-999999999999");
    private static final UUID MEMBER2 = UUID.fromString("eeeeeeee-8888-8888-8888-888888888888");

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

    private UUID orderId;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM product_review");
        jdbc.update("DELETE FROM order_line WHERE order_id IN (SELECT id FROM orders WHERE order_no LIKE 'RV-%')");
        jdbc.update("DELETE FROM orders WHERE order_no LIKE 'RV-%'");
        jdbc.update("DELETE FROM member WHERE id IN (?, ?)", MEMBER, MEMBER2);
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, 'M-RV-1', '评价用户', '13933330001', 'STANDARD', 0, 0, 'CNY'),
                       (?, 'M-RV-2', '另一用户', '13933330002', 'STANDARD', 0, 0, 'CNY')
                """, MEMBER, MEMBER2);
        jdbc.update("""
                INSERT INTO product (id, title, status) VALUES (?, '评价测试商品', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
                """, PRODUCT);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, 'SKU-REVIEW', 1000, 'CNY') ON CONFLICT (id) DO NOTHING
                """, SKU, PRODUCT);

        orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode,
                                    member_id)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, '评价用户', '13933330001', 'SHIP', ?)
                """, orderId, "RV-" + orderId, MEMBER);
        jdbc.update("""
                INSERT INTO order_line (id, order_id, sku_id, title_snapshot, sku_code_snapshot,
                                        quantity, unit_price_amount, currency, fulfillment_type)
                VALUES (?, ?, ?, '评价测试商品', 'SKU-REVIEW', 1, 1000, 'CNY', 'WAREHOUSE_SHIP')
                """, UUID.randomUUID(), orderId, SKU);
    }

    @Test
    void memberCanReviewPurchasedSkuOnce() {
        UUID id = reviews.create(MEMBER, orderId, SKU, 5, "很好用");

        assertThat(reviews.listForSku(SKU, 10)).extracting(ProductReviewService.Review::id).contains(id);
        assertThat(reviews.averageRating(SKU)).isEqualTo(5.0);

        assertThatThrownBy(() -> reviews.create(MEMBER, orderId, SKU, 4, "重复"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("已经评价过");
    }

    @Test
    void otherMemberCannotReviewSomeoneElsesOrder() {
        assertThatThrownBy(() -> reviews.create(MEMBER2, orderId, SKU, 5, "越权"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不属于当前会员");
    }

    @Test
    void skuNotInOrderIsRejected() {
        UUID otherSku = UUID.fromString("eeeeeeee-5555-6666-7777-888888888888");
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, 'SKU-OTHER', 1000, 'CNY') ON CONFLICT (id) DO NOTHING
                """, otherSku, PRODUCT);

        assertThatThrownBy(() -> reviews.create(MEMBER, orderId, otherSku, 5, "不在订单里"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不在此订单中");
    }

    @Test
    void unpaidOrderCannotBeReviewed() {
        jdbc.update("UPDATE orders SET status = 'PENDING_PAYMENT' WHERE id = ?", orderId);

        assertThatThrownBy(() -> reviews.create(MEMBER, orderId, SKU, 5, "未支付"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不可评价");
    }
}
