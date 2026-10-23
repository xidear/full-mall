package com.fullmall.catalog;

import com.fullmall.catalog.app.SkuMatrixMultiService;
import com.fullmall.catalog.app.SkuOptionsMigrationService;
import com.fullmall.catalog.app.SkuOptionsProjectionService;
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

/** 投影：关联表为准，JSON 由它生成；回填后两者一致。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SkuProjectionTest {

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
    SkuOptionsMigrationService migration;

    @Autowired
    SkuOptionsProjectionService projection;

    @Autowired
    SkuMatrixMultiService matrix;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void projectionIsRebuiltFromRelationalTables() {
        UUID productId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '投影测试', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency, options_json)
                VALUES (?, ?, ?, 1000, 'CNY', '{}'::jsonb)
                """, UUID.randomUUID(), productId, "PROJ-BASE-" + productId.toString().substring(0, 6));

        Map<String, List<String>> dims = new LinkedHashMap<>();
        dims.put("颜色", List.of("黑"));
        dims.put("存储", List.of("256G"));
        matrix.generate(productId, "PROJ", dims, List.of(), 1990);

        int updated = projection.rebuild(productId);
        assertThat(updated).isGreaterThanOrEqualTo(1);

        String options = jdbc.queryForObject("""
                SELECT s.options_json::text FROM sku s
                 WHERE s.product_id = ? AND s.sku_code = 'PROJ-黑-256G'
                """, String.class, productId);
        assertThat(options).contains("颜色").contains("存储").contains("256G");
    }

    @Test
    void backfillRebuildsProjectionToo() {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '回填投影', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency, options_json)
                VALUES (?, ?, ?, 1000, 'CNY', '{"颜色":"白"}'::jsonb)
                """, skuId, productId, "PROJ-BF-" + productId.toString().substring(0, 6));

        migration.backfillProduct(productId);

        String options = jdbc.queryForObject(
                "SELECT options_json::text FROM sku WHERE id = ?", String.class, skuId);
        assertThat(options).contains("颜色").contains("白");
    }
}
