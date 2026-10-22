package com.fullmall.catalog;

import com.fullmall.catalog.export.CostCsvExporter;
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

/** 成本导出：表头 + 分/元双列 + 逗号标题转义。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class CostCsvExporterTest {

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
    CostCsvExporter exporter;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void exportsHeaderAndRowsWithEscaping() {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '含,逗号 商品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, 'CSV-1', 3000, 'CNY')
                """, skuId, productId);
        jdbc.update("""
                INSERT INTO sku_cost (sku_id, quantity, amount, avg_cost_amount, stale)
                VALUES (?, 20, 24000, 1200, false)
                """, skuId);

        String csv = exporter.csv(10);

        assertThat(csv).startsWith(CostCsvExporter.HEADER);
        assertThat(csv).contains("CSV-1");
        assertThat(csv).as("标题含逗号必须加引号转义").contains("\"含,逗号 商品\"");
        assertThat(csv).as("同时给分与元").contains("1200,12.00");
        assertThat(csv).contains("false");
    }
}
