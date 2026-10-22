package com.fullmall.catalog;

import com.fullmall.catalog.app.SkuOptionsMigrationService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 回填：多维 options_json → 关联表；重复执行幂等；组合签名唯一。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SkuOptionsMigrationTest {

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
    JdbcTemplate jdbc;

    @Test
    void backfillsDimensionsValuesAndCombinationKey() {
        UUID productId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '手机', 'ACTIVE')", productId);
        UUID skuId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency, options_json)
                VALUES (?, ?, 'MIG-1', 1000, 'CNY', '{"颜色":"黑","存储":"256G"}'::jsonb)
                """, skuId, productId);

        var result = migration.backfillProduct(productId);

        assertThat(result.skus()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM product_option WHERE product_id = ?", Integer.class, productId))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM sku_option_value WHERE sku_id = ?", Integer.class, skuId))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT combination_key FROM sku WHERE id = ?",
                String.class, skuId)).isNotBlank();

        // 幂等：再跑一次不产生重复维度/值
        migration.backfillProduct(productId);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM product_option WHERE product_id = ?", Integer.class, productId))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM sku_option_value WHERE sku_id = ?", Integer.class, skuId))
                .isEqualTo(2);
    }
}
