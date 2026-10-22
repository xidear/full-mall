package com.fullmall.fulfillment;

import com.fullmall.fulfillment.infra.FulfillmentRepository;
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

/** 合并拣货行：映射表可回填**多张订单行**的批次（此前会留空导致退货无法回冲）。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class PickLineMappingTest {

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
    FulfillmentRepository fulfillment;

    @Autowired
    JdbcTemplate jdbc;

    private UUID order() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 2000, 0, 0, 2000, 'p', '13800000000', 'PICKUP')
                """, id, "PM-" + id.toString().substring(0, 8));
        return id;
    }

    private UUID line(UUID orderId, UUID skuId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO order_line (id, order_id, sku_id, title_snapshot, sku_code_snapshot,
                                        unit_price_amount, currency, fulfillment_type, quantity)
                VALUES (?, ?, ?, '商品', 'SKU-1', 1000, 'CNY', 'PICKUP', 1)
                """, id, orderId, skuId);
        return id;
    }

    @Test
    void mappingBackfillsBatchForMergedOrderLines() {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '映射品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1000, 'CNY')
                """, skuId, productId, "MAP-" + skuId.toString().substring(0, 8));

        UUID orderId = order();
        UUID paidLine = line(orderId, skuId);
        UUID giftLine = line(orderId, skuId);

        UUID storeId = UUID.randomUUID();
        jdbc.update("INSERT INTO market (code, name, currency, default_locale)"
                + " VALUES ('CN', '中国', 'CNY', 'zh-CN') ON CONFLICT (code) DO NOTHING");
        jdbc.update("""
                INSERT INTO store (id, market_code, name, address, active)
                VALUES (?, 'CN', '映射门店', '测试地址', true)
                """, storeId);
        UUID fulfillmentOrderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO fulfillment_order (id, order_id, type, status)
                VALUES (?, ?, 'STORE_PICKUP', 'PENDING')
                """, fulfillmentOrderId, orderId);
        UUID pickTaskId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO pick_task (id, fulfillment_order_id, store_id, status)
                VALUES (?, ?, ?, 'CREATED')
                """, pickTaskId, fulfillmentOrderId, storeId);
        UUID pickLineId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO pick_task_line (id, pick_task_id, sku_id, quantity, picked_quantity)
                VALUES (?, ?, ?, 2, 0)
                """, pickLineId, pickTaskId, skuId);

        fulfillment.insertPickLineMapping(pickLineId, paidLine, 1);
        fulfillment.insertPickLineMapping(pickLineId, giftLine, 1);

        UUID batchId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no, quantity)
                VALUES (?, ?, 'STORE', ?, ?, 2)
                """, batchId, skuId, storeId, "B-" + batchId.toString().substring(0, 8));
        int updated = fulfillment.recordOrderLineBatchForPickLine(pickLineId, batchId);

        assertThat(updated).as("两行都应回填").isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT batch_id FROM order_line WHERE order_id = ?",
                UUID.class, orderId)).containsOnly(batchId);
    }
}
