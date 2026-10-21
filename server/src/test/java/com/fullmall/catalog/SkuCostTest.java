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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SKU 成本：可维护、可清除、负数拒绝；未维护返回 null（不得当作 0）。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SkuCostTest {

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

    private UUID sku() {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '成本测试', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1000, 'CNY')
                """, skuId, productId, "COST-" + skuId.toString().substring(0, 8));
        return skuId;
    }

    @Test
    void costCanBeSetReadAndCleared() {
        UUID skuId = sku();
        assertThat(catalog.cost(skuId)).as("未维护时返回 null，不是 0").isNull();

        catalog.updateCost(skuId, 650L);
        assertThat(catalog.cost(skuId)).isEqualTo(650L);

        catalog.updateCost(skuId, null);
        assertThat(catalog.cost(skuId)).isNull();
    }

    @Test
    void negativeCostIsRejected() {
        UUID skuId = sku();
        assertThatThrownBy(() -> catalog.updateCost(skuId, -1L))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("成本不能为负数");
    }
}
