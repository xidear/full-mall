package com.fullmall.catalog;

import com.fullmall.catalog.app.SkuPriceRuleService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 按规格改价：只改命中规格、先预览、空筛选拒绝、结果为非正拒绝。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SkuPriceRuleTest {

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
    SkuPriceRuleService rules;

    @Autowired
    JdbcTemplate jdbc;

    private UUID phone(String tag) {
        UUID productId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '手机', 'ACTIVE')", productId);
        for (String color : new String[]{"黑", "白"}) {
            for (String storage : new String[]{"256G", "512G"}) {
                jdbc.update("""
                        INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency, options_json)
                        VALUES (?, ?, ?, 599900, 'CNY',
                                jsonb_build_object('颜色', ?, '存储', ?))
                        """, UUID.randomUUID(), productId,
                        tag + "-" + color + "-" + storage, color, storage);
            }
        }
        return productId;
    }

    @Test
    void onlyMatchingSpecIsChanged() {
        UUID productId = phone("T1");

        var preview = rules.preview(productId, Map.of("存储", "256G"),
                SkuPriceRuleService.Mode.ADD, 10_000);
        assertThat(preview).hasSize(2);
        assertThat(preview).allSatisfy(change -> {
            assertThat(change.before()).isEqualTo(599_900);
            assertThat(change.after()).isEqualTo(609_900);
        });

        assertThat(rules.apply(productId, Map.of("存储", "256G"),
                SkuPriceRuleService.Mode.ADD, 10_000)).isEqualTo(2);

        assertThat(jdbc.queryForList(
                "SELECT price_amount FROM sku WHERE product_id = ? AND sku_code LIKE '%256G'",
                Long.class, productId))
                .containsOnly(609_900L);
        assertThat(jdbc.queryForList(
                "SELECT price_amount FROM sku WHERE product_id = ? AND sku_code LIKE '%512G'",
                Long.class, productId))
                .as("未命中规格不得改动").containsOnly(599_900L);
    }

    @Test
    void rejectsEmptyFilterAndNonPositiveResult() {
        UUID productId = phone("T2");
        assertThatThrownBy(() -> rules.preview(productId, Map.of(),
                SkuPriceRuleService.Mode.ADD, 100))
                .hasMessageContaining("不允许无差别改价");

        assertThatThrownBy(() -> rules.preview(productId, Map.of("存储", "256G"),
                SkuPriceRuleService.Mode.ADD, -599_900))
                .hasMessageContaining("改价后价格必须大于 0");

        assertThatThrownBy(() -> rules.preview(productId, Map.of("存储", "1T"),
                SkuPriceRuleService.Mode.ADD, 100))
                .hasMessageContaining("没有匹配该规格条件");
    }
}
