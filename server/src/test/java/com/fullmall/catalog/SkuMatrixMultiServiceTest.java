package com.fullmall.catalog;

import com.fullmall.catalog.app.SkuMatrixMultiService;
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

/** 多维规格：笛卡尔积、组合禁用、多维 options_json、上限与幂等。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SkuMatrixMultiServiceTest {

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
    JdbcTemplate jdbc;

    private UUID product() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '手机', 'ACTIVE')", id);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 599900, 'CNY')
                """, UUID.randomUUID(), id, "PHONE-BASE-" + id.toString().substring(0, 6));
        return id;
    }

    private Map<String, List<String>> dimensions() {
        Map<String, List<String>> dims = new LinkedHashMap<>();
        dims.put("颜色", List.of("黑", "白"));
        dims.put("存储", List.of("256G", "512G"));
        return dims;
    }

    @Test
    void generatesCartesianProductWithMultiDimOptions() {
        UUID productId = product();

        var result = matrix.generate(productId, "PHONE", dimensions(), List.of(), 599_900);

        assertThat(result.created()).isEqualTo(4);
        assertThat(result.codes()).contains("PHONE-黑-256G", "PHONE-白-512G");
        String options = jdbc.queryForObject(
                "SELECT options_json::text FROM sku WHERE sku_code = 'PHONE-黑-512G'", String.class);
        assertThat(options).contains("颜色").contains("存储").contains("黑").contains("512G");
    }

    @Test
    void disabledCombinationIsSkipped() {
        UUID productId = product();
        var disabled = List.of(Map.of("颜色", "白", "存储", "512G"));

        var result = matrix.generate(productId, "PH2", dimensions(), disabled, 599_900);

        assertThat(result.created()).isEqualTo(3);
        assertThat(result.disabled()).isEqualTo(1);
        assertThat(result.codes()).doesNotContain("PH2-白-512G");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sku WHERE sku_code LIKE 'PH2-%'",
                Integer.class)).isEqualTo(3);
    }

    @Test
    void isIdempotentAndRejectsInvalidInput() {
        UUID productId = product();
        matrix.generate(productId, "PH3", dimensions(), List.of(), 599_900);
        var second = matrix.generate(productId, "PH3", dimensions(), List.of(), 899_900);
        assertThat(second.created()).isZero();
        assertThat(second.skipped()).isEqualTo(4);
        assertThat(jdbc.queryForObject(
                "SELECT price_amount FROM sku WHERE sku_code = 'PH3-黑-256G'", Long.class))
                .as("已存在编码不覆盖价格").isEqualTo(599_900L);

        assertThatThrownBy(() -> matrix.generate(productId, "PH4", Map.of(), List.of(), 1))
                .hasMessageContaining("至少需要一个规格维度");
        assertThatThrownBy(() -> matrix.generate(productId, "PH4", dimensions(), List.of(), 0))
                .hasMessageContaining("价格必须大于 0");
    }
}
