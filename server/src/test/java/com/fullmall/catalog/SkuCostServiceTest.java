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

    /** COST-3：退货按**原批次成本**回冲，而不是当前均价。 */
    @Test
    void returnUsesOriginalBatchCostNotCurrentAverage() {
        UUID skuId = sku();
        // 先按 10 元入库 10 件，再按 20 元入库 10 件 → 均价 15 元
        costs.receive(skuId, 10, 1_000, "RECEIPT", "R1");
        costs.receive(skuId, 10, 2_000, "RECEIPT", "R2");
        assertThat(costs.current(skuId).avgCost()).isEqualTo(1_500);

        // 造一个批次（成本 10 元），退货 5 件应按 10 元回冲
        UUID batchId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no,
                                             quantity, unit_cost_amount, unit_cost_currency)
                VALUES (?, ?, 'WAREHOUSE', ?, 'B-RET-1', 10, 1000, 'CNY')
                """, batchId, skuId, UUID.randomUUID());

        var after = costs.returnFromBatch(skuId, batchId, 5);

        // 数量 25、金额 20000+10000+5000 = 35000 → 均价 1400（若按均价 1500 回冲会得到 1500）
        assertThat(after.quantity()).isEqualTo(25);
        assertThat(after.amount()).isEqualTo(35_000);
        assertThat(after.avgCost()).isEqualTo(1_400);
    }

    @Test
    void returnWithoutBatchCostIsRejected() {
        UUID skuId = sku();
        UUID batchId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no, quantity)
                VALUES (?, ?, 'WAREHOUSE', ?, 'B-NOCOST', 5)
                """, batchId, skuId, UUID.randomUUID());

        assertThatThrownBy(() -> costs.returnFromBatch(skuId, batchId, 1))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("没有成本记录");
    }

    /** 只读视图：无成本时 unknown=true；有成本时返回最近批次来源。 */
    @Test
    void latestBatchProvidesTraceableSource() {
        UUID skuId = sku();
        assertThat(costs.latestBatch(skuId)).isNull();

        UUID batchId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no,
                                             quantity, unit_cost_amount, unit_cost_currency)
                VALUES (?, ?, 'WAREHOUSE', ?, 'PO-TRACE-1', 12, 880, 'CNY')
                """, batchId, skuId, UUID.randomUUID());
        costs.receive(skuId, 12, 880, "PURCHASE_RECEIPT", "PO-TRACE-1");

        var latest = costs.latestBatch(skuId);
        assertThat(latest).isNotNull();
        assertThat(latest.get("batchNo")).isEqualTo("PO-TRACE-1");
        assertThat(((Number) latest.get("unitCost")).longValue()).isEqualTo(880);
    }
}
