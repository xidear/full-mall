package com.fullmall.promotion;

import com.fullmall.promotion.ladder.PromotionLadderService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** C9 接入：开启后满赠 + 第二件半价生效；已有活动型优惠时跳过。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.promotion.ladder-enabled=true"
        })
@ActiveProfiles("test")
class PromotionLadderIntegrationTest {

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
    PromotionLadderService ladders;

    @Autowired
    JdbcTemplate jdbc;

    private UUID sku(long price) {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '阶梯商品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, ?, 'CNY')
                """, skuId, productId, "LAD-" + skuId.toString().substring(0, 6), price);
        return skuId;
    }

    private void giftLadder(UUID giftSku, long threshold) {
        jdbc.update("""
                INSERT INTO promotion_ladder (id, code, kind, threshold_fen, gift_sku_id, gift_quantity, priority)
                VALUES (?, ?, 'GIFT', ?, ?, 1, 10)
                """, UUID.randomUUID(), "GIFT-" + UUID.randomUUID().toString().substring(0, 6),
                threshold, giftSku);
    }

    private void secondHalf() {
        jdbc.update("""
                INSERT INTO promotion_ladder (id, code, kind, priority)
                VALUES (?, ?, 'SECOND_HALF', 5)
                """, UUID.randomUUID(), "HALF-" + UUID.randomUUID().toString().substring(0, 6));
    }

    @Test
    void giftAndSecondHalfApplyWhenEnabled() {
        UUID gift = sku(500);
        // 阈值取 19_000：高于共享库里其它用例的阶梯，避免互相干扰
        giftLadder(gift, 19_000);
        secondHalf();

        var result = ladders.evaluate(20_000, List.of(10_000L, 6_000L), false);

        assertThat(result.gifts()).anySatisfy(item -> assertThat(item.skuId()).isEqualTo(gift.toString()));
        assertThat(result.discountAmount()).isEqualTo(3_000);
        assertThat(result.appliedCode()).contains("GIFT").contains("SECOND_HALF");
    }

    @Test
    void skippedWhenCampaignDiscountExistsOrBelowThreshold() {
        UUID gift = sku(500);
        giftLadder(gift, 99_000);
        secondHalf();

        assertThat(ladders.evaluate(20_000, List.of(10_000L), true).appliedCode()).isNull();
        assertThat(ladders.evaluate(5_000, List.of(1_000L), false).gifts()).isEmpty();
    }
}
