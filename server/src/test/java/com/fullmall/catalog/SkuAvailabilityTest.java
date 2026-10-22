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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 可用量：多批次累加；无批次 SKU 显示 0（前台据此禁用缺货组合）。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SkuAvailabilityTest {

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
    void sumsBatchesAndShowsZeroWhenOutOfStock() {
        UUID productId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '手机', 'ACTIVE')", productId);
        UUID inStock = UUID.randomUUID();
        UUID outOfStock = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, 'AV-1', 1000, 'CNY'), (?, ?, 'AV-2', 2000, 'CNY')
                """, inStock, productId, outOfStock, productId);
        UUID locationId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no, quantity)
                VALUES (?, ?, 'WAREHOUSE', ?, 'B1', 3), (?, ?, 'WAREHOUSE', ?, 'B2', 4)
                """, UUID.randomUUID(), inStock, locationId, UUID.randomUUID(), inStock, locationId);

        var rows = catalog.availability(productId);

        assertThat(rows).anySatisfy(row -> {
            assertThat(row.get("skuCode")).isEqualTo("AV-1");
            assertThat(((Number) row.get("available")).intValue()).isEqualTo(7);
        });
        assertThat(rows).anySatisfy(row -> {
            assertThat(row.get("skuCode")).isEqualTo("AV-2");
            assertThat(((Number) row.get("available")).intValue()).as("无批次显示 0").isZero();
        });
    }
}
