package com.fullmall.catalog;

import com.fullmall.catalog.app.SkuMatrixMultiService;
import com.fullmall.catalog.app.SkuPriceRuleService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 规格改价（商品内、维度值集合）：维度间 AND、同维度多值 OR。
 *
 * <p>回归背景：原来的实现只支持"单维度单值"，且是跨商品的全局改价——
 * 200 个商品都有「存储=256G」时会被一起改掉（2026-10-28 用户指出）。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.rabbitmq.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SkuPriceRuleMultiTest {

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
    SkuMatrixMultiService matrix;

    @Autowired
    SkuPriceRuleService priceRules;

    @Autowired
    JdbcTemplate jdbc;

    private String prefixOf(UUID productId) {
        return "P" + productId.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }

    private UUID product() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '手机', 'ACTIVE')", id);
        LinkedHashMap<String, List<String>> dimensions = new LinkedHashMap<>();
        dimensions.put("颜色", List.of("红", "绿"));
        dimensions.put("存储", List.of("128G", "256G"));
        // sku_code 全局唯一：每个测试用独立前缀，避免 ON CONFLICT DO NOTHING 把后续测试的 SKU 吞掉
        String prefix = "P" + id.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        matrix.generate(id, prefix, dimensions, List.of(), 100_00L, "CNY");
        return id;
    }

    private long priceOf(String skuCode) {
        return jdbc.queryForObject("SELECT price_amount FROM sku WHERE sku_code = ?",
                Long.class, skuCode);
    }

    @Test
    void twoDimensionsIntersectAndValuesAreOr() {
        UUID productId = product();

        var changes = priceRules.previewMulti(productId,
                Map.of("颜色", List.of("红"), "存储", List.of("128G", "256G")),
                SkuPriceRuleService.Mode.ADD, 5_00L);

        String prefix = prefixOf(productId);
        assertThat(changes).hasSize(2);
        assertThat(changes).extracting(SkuPriceRuleService.Change::skuCode)
                .containsExactlyInAnyOrder(prefix + "-红-128G", prefix + "-红-256G");

        int updated = priceRules.applyMulti(productId,
                Map.of("颜色", List.of("红"), "存储", List.of("128G", "256G")),
                SkuPriceRuleService.Mode.ADD, 5_00L);

        assertThat(updated).isEqualTo(2);
        assertThat(priceOf(prefix + "-红-128G")).isEqualTo(105_00L);
        assertThat(priceOf(prefix + "-红-256G")).isEqualTo(105_00L);
        // 未勾选的组合不受影响
        assertThat(priceOf(prefix + "-绿-128G")).isEqualTo(100_00L);
        assertThat(priceOf(prefix + "-绿-256G")).isEqualTo(100_00L);
    }

    @Test
    void singleDimensionOnlyTouchesThatDimension() {
        UUID productId = product();

        var changes = priceRules.previewMulti(productId, Map.of("存储", List.of("128G")),
                SkuPriceRuleService.Mode.SET, 88_00L);

        assertThat(changes).hasSize(2);
        assertThat(changes).allMatch(change -> change.skuCode().endsWith("-128G"));
    }

    @Test
    void emptySelectionAndNegativeResultAreRejected() {
        UUID productId = product();

        assertThatThrownBy(() -> priceRules.previewMulti(productId, Map.of(),
                SkuPriceRuleService.Mode.ADD, 1_00L))
                .hasMessageContaining("至少勾选一个规格值");

        assertThatThrownBy(() -> priceRules.previewMulti(productId,
                Map.of("存储", List.of("128G", "256G")),
                SkuPriceRuleService.Mode.ADD, -500_00L))
                .hasMessageContaining("必须大于 0");
    }

    @Test
    void dimensionsExposeOnlyRealCombinations() {
        UUID productId = product();

        var dimensions = priceRules.dimensions(productId);

        // 维度顺序由 sort_order/名称决定，这里只断言"真实存在的组合都被暴露出来"
        assertThat(dimensions.keySet()).containsExactlyInAnyOrder("颜色", "存储");
        assertThat(dimensions.get("颜色")).containsExactlyInAnyOrder("红", "绿");
        assertThat(dimensions.get("存储")).containsExactlyInAnyOrder("128G", "256G");
    }
}
