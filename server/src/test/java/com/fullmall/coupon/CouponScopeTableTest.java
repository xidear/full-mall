package com.fullmall.coupon;

import com.fullmall.coupon.app.CouponService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 券范围：JSON 写入 → 触发器落到关联表 → 读取按关联表过滤。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class CouponScopeTableTest {

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
    CouponService coupons;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void scopeJsonIsProjectedIntoTableAndUsedForEligibility() {
        UUID skuA = sku();
        UUID skuB = sku();
        UUID templateId = UUID.randomUUID();
        UUID memberId = member();
        UUID couponId = UUID.randomUUID();

        jdbc.update("""
                INSERT INTO coupon_template (id, code, title, type, value, total_quantity,
                                             per_member_limit, validity_days, starts_at, ends_at,
                                             status, scope, scope_ids)
                VALUES (?, ?, 'SKU 券', 'AMOUNT', 500, 10, 1, 7,
                        now() - interval '1 day', now() + interval '7 days', 'ACTIVE',
                        'SKU', CAST(? AS jsonb))
                """, templateId, "SCOPE-" + templateId.toString().substring(0, 6),
                "[\"" + skuA + "\"]");
        jdbc.update("""
                INSERT INTO member_coupon (id, template_id, member_id, code, status, expires_at)
                VALUES (?, ?, ?, ?, 'UNUSED', now() + interval '7 days')
                """, couponId, templateId, memberId, "MC-" + couponId.toString().substring(0, 8));

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM coupon_template_scope WHERE template_id = ?",
                Integer.class, templateId)).isEqualTo(1);

        long eligible = coupons.eligibleAmount(couponId,
                Map.of(skuA, 1990L, skuB, 5000L));
        assertThat(eligible).as("只算命中的 SKU").isEqualTo(1990L);
    }

    private UUID sku() {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '券范围商品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1990, 'CNY')
                """, skuId, productId, "CS-" + skuId.toString().substring(0, 6));
        return skuId;
    }

    private UUID member() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO member (id, member_no, name, phone) VALUES (?, ?, '券会员', ?)",
                id, "M-" + id.toString().substring(0, 6), "137" + id.toString().substring(0, 8));
        return id;
    }
}
