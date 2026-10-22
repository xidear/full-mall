package com.fullmall.catalog;

import com.fullmall.catalog.app.CatalogService;
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

/** 批量改价：整批成功、非法价格或不存在 SKU 整批失败（不做部分成功）。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class BatchPriceTest {

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
    CatalogService catalog;

    @Autowired
    JdbcTemplate jdbc;

    private UUID sku(String code, long price) {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '批量改价', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, ?, 'CNY')
                """, skuId, productId, code, price);
        return skuId;
    }

    @Test
    void updatesAllOrNothing() {
        UUID a = sku("BP-A", 1_000);
        UUID b = sku("BP-B", 2_000);

        int updated = catalog.batchUpdatePrices(Map.of(a, 1_500L, b, 2_500L));

        assertThat(updated).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT price_amount FROM sku WHERE id = ?", Long.class, a))
                .isEqualTo(1_500L);
        assertThat(jdbc.queryForObject("SELECT price_amount FROM sku WHERE id = ?", Long.class, b))
                .isEqualTo(2_500L);
    }

    @Test
    void invalidPriceOrMissingSkuFailsWholeBatch() {
        UUID a = sku("BP-C", 1_000);
        assertThatThrownBy(() -> catalog.batchUpdatePrices(Map.of(a, 0L)))
                .hasMessageContaining("价格必须大于 0");

        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> catalog.batchUpdatePrices(Map.of(a, 1_200L, missing, 1_300L)))
                .hasMessageContaining("SKU 不存在");
        assertThat(jdbc.queryForObject("SELECT price_amount FROM sku WHERE id = ?", Long.class, a))
                .as("整批失败时不应改动任何 SKU").isEqualTo(1_000L);
    }
}
