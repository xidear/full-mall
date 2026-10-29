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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 券的适用范围：计算口径 + 真实结算行为。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class CouponScopeTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final UUID PRODUCT = UUID.fromString("cfcfcfcf-0000-0000-0000-000000000001");
    private static final UUID SKU_A = UUID.fromString("cfcfcfcf-0000-0000-0000-000000000002");
    private static final UUID SKU_B = UUID.fromString("cfcfcfcf-0000-0000-0000-000000000003");
    private UUID memberId;
    private String phone;
    private static final UUID WAREHOUSE = UUID.fromString("cfcfcfcf-0000-0000-0000-000000000005");


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
    CouponService coupons;

    @Autowired
    OrderService orders;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        // 先删引用了模板的券，再删模板（外键顺序）
        jdbc.update("DELETE FROM member_coupon WHERE template_id IN"
                + " (SELECT id FROM coupon_template WHERE code LIKE 'SC-%')");
        jdbc.update("DELETE FROM coupon_template WHERE code LIKE 'SC-%'");
        // 每条用例独立会员与手机号：订单会引用会员，删不掉
        memberId = UUID.randomUUID();
        phone = "139" + String.format("%08d", Math.abs(memberId.hashCode() % 100_000_000));
        jdbc.update("""
                INSERT INTO market (code, name, currency, country_code, active)
                VALUES ('SC-M', '范围券市场', 'CNY', 'CN', TRUE) ON CONFLICT (code) DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO warehouse (id, market_code, name, type, active)
                VALUES (?, 'SC-M', '范围券仓', 'SELF', TRUE) ON CONFLICT (id) DO NOTHING
                """, WAREHOUSE);
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, ?, '范围券用户', ?, 'STANDARD', 0, 0, 'CNY')
                ON CONFLICT (id) DO NOTHING
                """, memberId, "M-SC-" + memberId.toString().substring(0, 8), phone);
        jdbc.update("""
                INSERT INTO product (id, title, status) VALUES (?, '范围券商品', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
                """, PRODUCT);
        String[][] skus = {{SKU_A.toString(), "SKU-SC-A"}, {SKU_B.toString(), "SKU-SC-B"}};
        for (String[] sku : skus) {
            jdbc.update("""
                    INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                    VALUES (?, ?, ?, 1000, 'CNY') ON CONFLICT (id) DO UPDATE SET sku_code = EXCLUDED.sku_code
                    """, UUID.fromString(sku[0]), PRODUCT, sku[1]);
            jdbc.update("""
                    INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no,
                                                quantity, reserved_quantity, status)
                    VALUES (?, ?, 'WAREHOUSE', ?, ?, 100, 0, 'AVAILABLE')
                    """, UUID.randomUUID(), UUID.fromString(sku[0]), WAREHOUSE,
                    "SCB-" + UUID.randomUUID());
        }
    }

    private UUID claimScopedCoupon(String code, String scope, List<UUID> ids) {
        UUID templateId = coupons.createTemplate(code, "范围券", "AMOUNT", 500, 0, 10, 1, 30,
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(86_400), scope, ids);
        coupons.activate(templateId);
        coupons.claim(templateId, memberId);
        return coupons.listMine(memberId, null, 10).get(0).id();
    }

    private CheckoutRequest request(UUID skuId, UUID couponId) {
        return new CheckoutRequest(
                List.of(new CheckoutRequest.Item(skuId, 1)),
                FulfillmentMode.SHIP,
                null,
                new CheckoutRequest.Contact("范围券用户", phone, null),
                new CheckoutRequest.Address("CN", "上海", "上海", "静安", "南京西路 1 号", "200040"),
                // legalAcceptance, referrerId, referralMode, groupBuyId, pointsToUse, couponId, otp, placedBy, operator
                List.of(), null, null, null, new CheckoutRequest.CheckoutContext(null, couponId, null, null, null), null);
    }

    @Test
    void skuScopedCouponCountsOnlyMatchingSku() {
        UUID couponId = claimScopedCoupon("SC-1", "SKU", List.of(SKU_A));

        long eligible = coupons.eligibleAmount(couponId,
                Map.of(SKU_A, 1_000L, SKU_B, 2_000L));

        assertThat(eligible).isEqualTo(1_000);
    }

    @Test
    void allScopeCountsEverything() {
        UUID couponId = claimScopedCoupon("SC-2", "ALL", List.of());

        assertThat(coupons.eligibleAmount(couponId, Map.of(SKU_A, 1_000L, SKU_B, 2_000L)))
                .isEqualTo(3_000);
    }

    @Test
    void scopedCouponHasZeroEligibleWhenNoMatchingSku() {
        UUID couponId = claimScopedCoupon("SC-4", "SKU", List.of(SKU_A));

        long eligible = coupons.eligibleAmount(couponId, Map.of(SKU_B, 2_000L));

        assertThat(eligible).isZero();
        // 门槛为 0 的券对 0 金额仍判"可用"（折扣自然为 0），
        // 因此结算层必须显式拒绝 `eligible == 0`，否则会出现"用了券但没优惠"的静默失败。
        assertThat(coupons.checkUsable(couponId, memberId, eligible).discountAmount()).isZero();
    }

    @Test
    void checkoutRejectsCouponWhoseScopeMatchesNothing() {
        UUID couponId = claimScopedCoupon("SC-5", "SKU", List.of(SKU_A));

        // 购物车里只有 SKU_B，券只覆盖 SKU_A → 结算必须明确拒绝
        assertThatThrownBy(() -> orders.checkout(request(SKU_B, couponId), null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("没有该券可用的商品");
    }

    @Test
    void checkoutAppliesCouponWhenScopeMatches() {
        UUID couponId = claimScopedCoupon("SC-6", "SKU", List.of(SKU_A));

        var order = orders.checkout(request(SKU_A, couponId), null);

        assertThat(jdbc.queryForObject(
                "SELECT discount_amount FROM orders WHERE id = ?", Long.class, order.id()))
                .isEqualTo(500);
    }

    @Test
    void nonAllScopeRequiresIds() {
        assertThatThrownBy(() -> coupons.createTemplate("SC-3", "范围券", "AMOUNT", 500, 0, 10, 1, 30,
                Instant.now(), Instant.now().plusSeconds(60), "SKU", List.of()))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("必须指定范围 ID");
    }
}