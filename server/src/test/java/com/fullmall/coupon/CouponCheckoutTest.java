package com.fullmall.coupon;

import com.fullmall.coupon.app.CouponService;
import com.fullmall.order.api.CheckoutRequest;
import com.fullmall.order.app.OrderService;
import com.fullmall.order.domain.FulfillmentMode;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.promotion.coupon.mode=EXCLUSIVE",
                "app.payment.provider=mock"
        })
@ActiveProfiles("test")
class CouponCheckoutTest {

    private static final EmbeddedPostgres POSTGRES;
    private UUID memberId;
    private String phone;
    private static final UUID PRODUCT = UUID.fromString("cccccccc-1111-0000-0000-0000000000bb");
    private static final UUID SKU = UUID.fromString("cccccccc-1111-0000-0000-0000000000cc");
    private static final UUID WAREHOUSE = UUID.fromString("cccccccc-1111-0000-0000-0000000000dd");
    private UUID batchId;

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
    CouponService coupons;

    @Autowired
    com.fullmall.payment.app.PaymentService payments;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM member_coupon");
        jdbc.update("DELETE FROM coupon_template WHERE code LIKE 'CK-%'");
        jdbc.update("DELETE FROM promotion WHERE code LIKE 'CK-%'");
        batchId = java.util.UUID.randomUUID();
        // 每条用例独立会员与手机号，避免删除历史订单时的外键连锁
        memberId = UUID.randomUUID();
        phone = "139" + String.format("%08d", Math.abs(memberId.hashCode() % 100_000_000));
        jdbc.update("""
                INSERT INTO market (code, name, currency, default_locale, active)
                VALUES ('CN-CK', '券测试市场', 'CNY', 'zh-CN', TRUE) ON CONFLICT (code) DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO warehouse (id, market_code, name, type, active)
                VALUES (?, 'CN-CK', '券测试仓', 'SELF', TRUE) ON CONFLICT (id) DO NOTHING
                """, WAREHOUSE);
        jdbc.update("""
                INSERT INTO product (id, title, status) VALUES (?, '券测试商品', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
                """, PRODUCT);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, 'SKU-CK', 2000, 'CNY') ON CONFLICT (id) DO NOTHING
                """, SKU, PRODUCT);
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no,
                                            quantity, reserved_quantity, status)
                VALUES (?, ?, 'WAREHOUSE', ?, ?, 50, 0, 'AVAILABLE')
                """, batchId, SKU, WAREHOUSE, "CK-" + batchId);
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, ?, '券结算用户', ?, 'STANDARD', 0, 0, 'CNY')
                """, memberId, "M-CK-" + memberId.toString().substring(0, 8).toUpperCase(), phone);
    }

    private UUID claimCoupon() {
        String code = "CK-" + memberId.toString().substring(0, 8).toUpperCase();
        UUID templateId = coupons.createTemplate(code, "满 10 减 5", "AMOUNT", 500, 0, 10, 1, 30,
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(86_400));
        coupons.activate(templateId);
        coupons.claim(templateId, memberId);
        return coupons.listMine(memberId, null, 10).get(0).id();
    }

    private CheckoutRequest request(UUID couponId) {
        return new CheckoutRequest(
                List.of(new CheckoutRequest.Item(SKU, 1)),
                FulfillmentMode.SHIP,
                null,
                new CheckoutRequest.Contact("券结算用户", phone, null),
                new CheckoutRequest.Address("上海", "上海", "静安", "南京西路 9 号", "200040"),
                List.of(), null, null, null, null, couponId);
    }

    @Test
    void couponDiscountAppliesAndCouponIsConsumed() {
        UUID couponId = claimCoupon();

        var order = orders.checkout(request(couponId), null);

        assertThat(jdbc.queryForObject(
                "SELECT discount_amount FROM orders WHERE id = ?", Long.class, order.id())).isEqualTo(500);
        assertThat(jdbc.queryForObject(
                "SELECT total_amount FROM orders WHERE id = ?", Long.class, order.id())).isEqualTo(1_500);
        assertThat(jdbc.queryForMap(
                "SELECT status, order_id FROM member_coupon WHERE id = ?", couponId))
                .containsEntry("status", "USED")
                .containsEntry("order_id", order.id());
    }

    @Test
    void fullRefundReturnsCouponToUnused() {
        UUID couponId = claimCoupon();
        var order = orders.checkout(request(couponId), null);
        payments.payMock(order.id());

        payments.refund(order.id(), order.total().amount());

        assertThat(jdbc.queryForMap(
                "SELECT status, order_id, used_at FROM member_coupon WHERE id = ?", couponId))
                .containsEntry("status", "UNUSED")
                .containsEntry("order_id", null)
                .containsEntry("used_at", null);
    }

    @Test
    void refundOfExpiredCouponMarksItExpired() {
        UUID couponId = claimCoupon();
        var order = orders.checkout(request(couponId), null);
        payments.payMock(order.id());
        jdbc.update("UPDATE member_coupon SET expires_at = now() - interval '1 minute' WHERE id = ?", couponId);

        payments.refund(order.id(), order.total().amount());

        assertThat(jdbc.queryForObject(
                "SELECT status FROM member_coupon WHERE id = ?", String.class, couponId)).isEqualTo("EXPIRED");
    }

    @Test
    void exclusiveModeRejectsCouponThatIsWorseThanPromotion() {
        // 促销：满 1000 减 800（大于券面额 500）
        jdbc.update("""
                INSERT INTO promotion (id, code, type, title, status, starts_at, ends_at, rules, priority)
                VALUES (?, ?, 'FULL_REDUCTION', '测试满减', 'ACTIVE',
                        now() - interval '1 hour', now() + interval '1 day',
                        '{"thresholdAmount":1000,"value":800}'::jsonb, 10)
                """, UUID.randomUUID(), "CK-PROMO-" + memberId.toString().substring(0, 8).toUpperCase());
        UUID couponId = claimCoupon();

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> orders.checkout(request(couponId), null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("小于当前促销优惠");

        // 券未被吃掉
        assertThat(jdbc.queryForObject(
                "SELECT status FROM member_coupon WHERE id = ?", String.class, couponId)).isEqualTo("UNUSED");
    }

    @Test
    void sameCouponCannotBeUsedTwice() {
        UUID couponId = claimCoupon();
        orders.checkout(request(couponId), null);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> orders.checkout(request(couponId), null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不可用");
    }
}
