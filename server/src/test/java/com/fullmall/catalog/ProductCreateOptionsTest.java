package com.fullmall.catalog;

import com.fullmall.catalog.app.CatalogService;
import com.fullmall.catalog.api.ProductUpsertRequest;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 建商品时规格即写入关联表（不再只写 JSON）。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ProductCreateOptionsTest {

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
    void createProductWritesRelationalOptions() {
        String tag = UUID.randomUUID().toString().substring(0, 6);
        var sku = new ProductUpsertRequest.SkuUpsert(
                "OPT-" + tag, null, 1990L, "CNY", null,
                Map.of("颜色", "黑"), null, null);
        var product = catalog.createProduct(new ProductUpsertRequest(
                "选项商品 " + tag, "描述", null, null,
                com.fullmall.catalog.domain.InventoryTrackingLevel.VARIANT, false, List.of(sku)));

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM sku_option_value v JOIN sku s ON s.id = v.sku_id WHERE s.product_id = ?",
                Integer.class, product.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM product_option WHERE product_id = ?", Integer.class, product.id()))
                .isEqualTo(1);
    }
}
