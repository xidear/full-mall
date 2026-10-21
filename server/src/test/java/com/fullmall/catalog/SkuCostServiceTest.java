package com.fullmall.catalog;

import com.fullmall.catalog.app.SkuCostService;
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

/** COST-1/2/4：入库重算均价、出库不改均价、无成本记录明确拒绝。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SkuCostServiceTest {

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
    SkuCostService costs;

    @Autowired
    JdbcTemplate jdbc;

    private UUID sku() {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '成本', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 2000, 'CNY')
                """, skuId, productId, "CST-" + skuId.toString().substring(0, 8));
        return skuId;
    }

    @Test
    void receiptsRecomputeAverageAndIssuesKeepIt() {
        UUID skuId = sku();
        assertThat(costs.current(skuId)).as("从未入库").isNull();

        costs.receive(skuId, 10, 1_000, "RECEIPT", "R1");
        costs.receive(skuId, 10, 1_400, "RECEIPT", "R2");

        var afterReceipts = costs.current(skuId);
        assertThat(afterReceipts.avgCost()).isEqualTo(1_200);
        assertThat(afterReceipts.quantity()).isEqualTo(20);
        assertThat(afterReceipts.amount()).isEqualTo(24_000);

        var afterIssue = costs.issue(skuId, 5, "ORDER", "SO-1");
        assertThat(afterIssue.quantity()).isEqualTo(15);
        assertThat(afterIssue.amount()).isEqualTo(18_000);
        assertThat(afterIssue.avgCost()).isEqualTo(1_200);

        // 流水可追溯：2 条入库 + 1 条出库
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_cost_movement WHERE sku_id = ?", Integer.class, skuId))
                .isEqualTo(3);
    }

    @Test
    void issuingWithoutCostRecordIsRejected() {
        UUID skuId = sku();

        assertThatThrownBy(() -> costs.issue(skuId, 1, "ORDER", "SO-2"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("没有成本记录");
    }
}
