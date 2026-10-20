package com.fullmall.coupon;

import com.fullmall.coupon.app.CouponService;
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

/** 券的适用范围：只累加命中范围的商品金额。 */
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
    private static final UUID MEMBER = UUID.fromString("cfcfcfcf-0000-0000-0000-000000000004");

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
    JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM member_coupon WHERE member_id = ?", MEMBER);
        jdbc.update("DELETE FROM coupon_template WHERE code LIKE 'SC-%'");
        jdbc.update("DELETE FROM member WHERE id = ?", MEMBER);
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, 'M-SC-1', '范围券用户', '13900009999', 'STANDARD', 0, 0, 'CNY')
                ON CONFLICT (id) DO NOTHING
                """, MEMBER);
        jdbc.update("""
                INSERT INTO product (id, title, status) VALUES (?, '范围券商品', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
                """, PRODUCT);
        for (String[] sku : new String[][]{{SKU_A.toString(), "SKU-SC-A"}, {SKU_B.toString(), "SKU-SC-B"}}) {
            jdbc.update("""
                    INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                    VALUES (?, ?, ?, 1000, 'CNY') ON CONFLICT (id) DO UPDATE SET sku_code = EXCLUDED.sku_code
                    """, UUID.fromString(sku[0]), PRODUCT, sku[1]);
        }
    }

    private UUID claimScopedCoupon(String code, String scope, List<UUID> ids) {
        UUID templateId = coupons.createTemplate(code, "范围券", "AMOUNT", 500, 0, 10, 1, 30,
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(86_400), scope, ids);
        coupons.activate(templateId);
        coupons.claim(templateId, MEMBER);
        return coupons.listMine(MEMBER, null, 10).get(0).id();
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
        // 注意：门槛为 0 的券对 0 金额仍判"可用"（折扣自然为 0），
        // 因此结算层必须显式拒绝 `eligible == 0`，否则会出现"用了券但没优惠"的静默失败。
        assertThat(coupons.checkUsable(couponId, MEMBER, eligible).discountAmount()).isZero();
    }

    @Test
    void nonAllScopeRequiresIds() {
        assertThatThrownBy(() -> coupons.createTemplate("SC-3", "范围券", "AMOUNT", 500, 0, 10, 1, 30,
                Instant.now(), Instant.now().plusSeconds(60), "SKU", List.of()))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("必须指定范围 ID");
    }
}
