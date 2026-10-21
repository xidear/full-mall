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
class CouponServiceTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final UUID MEMBER = UUID.fromString("cccccccc-0000-0000-0000-0000000000aa");
    private static final UUID MEMBER2 = UUID.fromString("cccccccc-0000-0000-0000-0000000000bb");

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
        jdbc.update("DELETE FROM member_coupon");
        jdbc.update("DELETE FROM coupon_template WHERE code LIKE 'CP-%'");
        jdbc.update("DELETE FROM member WHERE id IN (?, ?)", MEMBER, MEMBER2);
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, 'M-CP-1', '券用户', '13977770001', 'STANDARD', 0, 0, 'CNY'),
                       (?, 'M-CP-2', '券用户2', '13977770002', 'STANDARD', 0, 0, 'CNY')
                """, MEMBER, MEMBER2);
    }

    private UUID activeTemplate(String code, int total, int perMember) {
        UUID id = coupons.createTemplate(code, "测试券", "AMOUNT", 500, 1_000, total, perMember, 30,
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(86_400));
        coupons.activate(id);
        return id;
    }

    @Test
    void claimIssuesUnusedCouponWithExpiry() {
        UUID templateId = activeTemplate("CP-1", 10, 1);

        coupons.claim(templateId, MEMBER);

        var mine = coupons.listMine(MEMBER, "UNUSED", 10);
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).status()).isEqualTo("UNUSED");
        assertThat(mine.get(0).expiresAt()).isAfter(Instant.now());
    }

    @Test
    void perMemberLimitIsEnforced() {
        UUID templateId = activeTemplate("CP-2", 10, 1);
        coupons.claim(templateId, MEMBER);

        assertThatThrownBy(() -> coupons.claim(templateId, MEMBER))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("每人限领");
    }

    @Test
    void soldOutWhenStockExhausted() {
        UUID templateId = activeTemplate("CP-3", 1, 5);
        coupons.claim(templateId, MEMBER);

        assertThatThrownBy(() -> coupons.claim(templateId, MEMBER2))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("已领完");
        assertThat(jdbc.queryForObject(
                "SELECT issued_quantity FROM coupon_template WHERE id = ?", Integer.class, templateId))
                .isEqualTo(1);
    }

    @Test
    void inactiveTemplateCannotBeClaimed() {
        UUID id = coupons.createTemplate("CP-4", "未启用券", "AMOUNT", 500, 0, 5, 1, 30,
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(86_400));

        assertThatThrownBy(() -> coupons.claim(id, MEMBER))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("未开始发放");
    }

    @Test
    void eligibilityReportsReasonAndDiscountWithoutChangingState() {
        UUID templateId = activeTemplate("CP-6", 10, 1);
        coupons.claim(templateId, MEMBER);
        UUID couponId = coupons.listMine(MEMBER, null, 10).get(0).id();

        // 未达门槛
        assertThat(coupons.checkUsable(couponId, MEMBER, 500).usable()).isFalse();
        assertThat(coupons.checkUsable(couponId, MEMBER, 500).reason()).contains("门槛");
        // 达到门槛（门槛 1000）：面额 500
        assertThat(coupons.checkUsable(couponId, MEMBER, 1_200).discountAmount()).isEqualTo(500);
        // 只读：券仍是 UNUSED
        assertThat(jdbc.queryForObject(
                "SELECT status FROM member_coupon WHERE id = ?", String.class, couponId)).isEqualTo("UNUSED");
    }

    @Test
    void amountDiscountNeverExceedsOrderAmount() {
        // 面额 2000、门槛 0，订单 1500 → 最多抵 1500
        UUID templateId = coupons.createTemplate("CP-9", "大额券", "AMOUNT", 2_000, 0, 10, 1, 30,
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(86_400));
        coupons.activate(templateId);
        coupons.claim(templateId, MEMBER);
        UUID couponId = coupons.listMine(MEMBER, null, 10).get(0).id();

        assertThat(coupons.checkUsable(couponId, MEMBER, 1_500).discountAmount()).isEqualTo(1_500);
    }

    @Test
    void percentCouponRoundsHalfUp() {
        UUID templateId = coupons.createTemplate("CP-7", "九折券", "PERCENT", 90, 0, 10, 1, 30,
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(86_400));
        coupons.activate(templateId);
        coupons.claim(templateId, MEMBER);
        UUID couponId = coupons.listMine(MEMBER, null, 10).get(0).id();

        // 3333 × 10% = 333.3 → 333
        assertThat(coupons.checkUsable(couponId, MEMBER, 3_333).discountAmount()).isEqualTo(333);
    }

    @Test
    void otherMembersCouponIsRejected() {
        UUID templateId = activeTemplate("CP-8", 10, 1);
        coupons.claim(templateId, MEMBER);
        UUID couponId = coupons.listMine(MEMBER, null, 10).get(0).id();

        assertThatThrownBy(() -> coupons.checkUsable(couponId, MEMBER2, 5_000))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不属于当前会员");
    }

    @Test
    void listTemplatesShowsIssuedCounts() {
        UUID templateId = activeTemplate("CP-LIST", 5, 2);
        coupons.claim(templateId, MEMBER);

        var templates = coupons.listTemplates(20);

        assertThat(templates).anySatisfy(view -> {
            assertThat(view.code()).isEqualTo("CP-LIST");
            assertThat(view.totalQuantity()).isEqualTo(5);
            assertThat(view.issuedQuantity()).isEqualTo(1);
            assertThat(view.status()).isEqualTo("ACTIVE");
        });
    }

    @Test
    void expiredCouponShowsAsExpiredEvenWithoutScheduler() {
        UUID templateId = activeTemplate("CP-5", 10, 1);
        coupons.claim(templateId, MEMBER);
        jdbc.update("UPDATE member_coupon SET expires_at = now() - interval '1 minute' WHERE member_id = ?",
                MEMBER);

        var mine = coupons.listMine(MEMBER, null, 10);
        assertThat(mine).singleElement()
                .satisfies(coupon -> assertThat(coupon.status()).isEqualTo("EXPIRED"));
    }

    @Test
    void consumeWritesAuditEvent() {
        UUID templateId = activeTemplate("CP-AUDIT", 3, 1);
        UUID couponId = coupons.claim(templateId, MEMBER);

        coupons.consume(couponId, MEMBER, UUID.randomUUID());

        var events = jdbc.queryForList(
                "SELECT actor FROM audit_event WHERE event_type = 'COUPON_REDEEMED' AND resource_id = ?",
                couponId.toString());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).get("actor")).isEqualTo("member:" + MEMBER);
    }

    /** 草稿可改；生效中不可改；停用后不可再领；已领取的券不受影响。 */
    @Test
    void draftCanBeEditedButActiveCannot() {
        UUID draft = coupons.createTemplate("CP-EDIT-1", "可编辑券", "AMOUNT", 500, 1_000, 10, 1, 30,
                Instant.now(), Instant.now().plusSeconds(86_400 * 30), "ALL", java.util.List.of());
        coupons.updateTemplate(draft, "改后标题", 800, 2_000, 20, 2, 15);
        var row = jdbc.queryForMap("SELECT title, value, min_order_amount, total_quantity"
                + " FROM coupon_template WHERE id = ?", draft);
        assertThat(row.get("title")).isEqualTo("改后标题");
        assertThat(((Number) row.get("value")).longValue()).isEqualTo(800);

        UUID active = activeTemplate("CP-EDIT-2", 10, 1);
        assertThatThrownBy(() -> coupons.updateTemplate(active, "改", 800, 0, 5, 1, 10))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("只有草稿状态");
    }

    @Test
    void disabledTemplateStopsIssuingButKeepsIssuedCoupons() {
        UUID active = activeTemplate("CP-DIS-1", 10, 1);
        UUID couponId = coupons.claim(active, MEMBER);

        coupons.disable(active);

        assertThat(jdbc.queryForObject("SELECT status FROM coupon_template WHERE id = ?",
                String.class, active)).isEqualTo("DISABLED");
        // 已领取的券仍然有效
        assertThat(jdbc.queryForObject("SELECT status FROM member_coupon WHERE id = ?",
                String.class, couponId)).isEqualTo("UNUSED");
        // 停用后不能再领
        assertThatThrownBy(() -> coupons.claim(active, MEMBER2))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class);
    }
}
