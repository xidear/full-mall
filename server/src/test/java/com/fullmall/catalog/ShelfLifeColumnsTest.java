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

/** V107：效期策略 JSON 写入 → 列同步 → 读取走列。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ShelfLifeColumnsTest {

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

    @Test
    void jsonWritePopulatesColumnsAndReadUsesThem() {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '效期商品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency,
                                 shelf_life_policy_json)
                VALUES (?, ?, ?, 1000, 'CNY', CAST(? AS jsonb))
                """, skuId, productId, "SL-" + skuId.toString().substring(0, 6),
                "{\"batchTracking\":true,\"pickingStrategy\":\"FEFO\","
                        + "\"minRemainingShelfLifeDays\":30,\"nearExpiryThresholdDays\":7,"
                        + "\"nearExpiryAction\":\"DISCOUNT\",\"autoBlockExpired\":false}");

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT shelf_batch_tracking, shelf_picking_strategy, shelf_min_remaining_days,
                       shelf_near_expiry_days, shelf_near_expiry_action, shelf_auto_block_expired
                  FROM sku WHERE id = ?
                """, skuId);
        assertThat(row.get("shelf_batch_tracking")).isEqualTo(true);
        assertThat(row.get("shelf_picking_strategy")).isEqualTo("FEFO");
        assertThat(row.get("shelf_min_remaining_days")).isEqualTo(30);
        assertThat(row.get("shelf_near_expiry_action")).isEqualTo("DISCOUNT");
        assertThat(row.get("shelf_auto_block_expired")).isEqualTo(false);

        var sku = catalog.getSku(skuId);
        assertThat(sku.shelfLifePolicy().minRemainingShelfLifeDays()).isEqualTo(30);
        assertThat(sku.shelfLifePolicy().pickingStrategy()).hasToString("FEFO");
    }
}
