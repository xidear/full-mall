package com.fullmall.promotion;

import com.fullmall.promotion.app.PromotionPreviewService;
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

/** C1/C2：默认取最优；配置成 STACK 后两条满减叠加。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.promotion.stacking-mode=STACK"
        })
@ActiveProfiles("test")
class PromotionStackingConfigTest {

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
    PromotionPreviewService preview;

    @Autowired
    JdbcTemplate jdbc;

    private UUID sku(long priceFen) {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '叠加测试', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, ?, 'CNY')
                """, skuId, productId, "STK-" + skuId.toString().substring(0, 6), priceFen);
        return skuId;
    }

    private void rule(String code, long threshold, long value, int priority) {
        jdbc.update("""
                INSERT INTO promotion (id, code, title, type, status, starts_at, ends_at, priority, rules)
                VALUES (?, ?, ?, 'FULL_REDUCTION', 'ACTIVE',
                        now() - interval '1 hour', now() + interval '1 day', ?, ?::jsonb)
                """, UUID.randomUUID(), code, code, priority,
                "{\"thresholdAmount\":" + threshold + ",\"value\":" + value
                        + ",\"channel\":\"ONLINE\"}");
    }

    @Test
    void stackModeAppliesBothRules() {
        UUID skuId = sku(20_000);
        String tag = UUID.randomUUID().toString().substring(0, 6);
        rule("STK-A-" + tag, 10_000, 1_000, 10);
        rule("STK-B-" + tag, 15_000, 2_000, 5);

        var result = preview.preview(List.of(new PromotionPreviewService.Item(skuId, 1)), "zh-CN", "ONLINE");

        assertThat(result.discountAmount()).isEqualTo(3_000);
        assertThat(result.appliedRuleCode()).contains("STK-A-" + tag).contains("STK-B-" + tag);
    }
}
