package com.fullmall.order;

import com.fullmall.order.infra.OrderRepository;
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

/** 订单行批次：可写入一次、不可被覆盖（防止退货回冲到错误批次）。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class OrderLineBatchTest {

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
    OrderRepository orders;

    @Autowired
    JdbcTemplate jdbc;

    private UUID line() {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '批次', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1000, 'CNY')
                """, skuId, productId, "LB-" + skuId.toString().substring(0, 8));
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, 'p', '13800000000', 'PICKUP')
                """, orderId, "LB-" + orderId.toString().substring(0, 8));
        jdbc.update("""
                INSERT INTO order_line (id, order_id, sku_id, title_snapshot, sku_code_snapshot,
                                        unit_price_amount, currency, fulfillment_type, quantity)
                VALUES (?, ?, ?, '批次', 'SKU-LB', 1000, 'CNY', 'PICKUP', 1)
                """, lineId, orderId, skuId);
        return lineId;
    }

    private UUID batch(UUID lineId) {
        UUID skuId = jdbc.queryForObject("SELECT sku_id FROM order_line WHERE id = ?",
                UUID.class, lineId);
        UUID batchId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no, quantity)
                VALUES (?, ?, 'STORE', ?, ?, 10)
                """, batchId, skuId, UUID.randomUUID(), "B-" + batchId.toString().substring(0, 8));
        return batchId;
    }

    @Test
    void batchIsRecordedOnceAndNotOverwritten() {
        UUID lineId = line();
        UUID first = batch(lineId);
        UUID second = batch(lineId);

        assertThat(orders.recordLineBatch(lineId, first)).isTrue();
        assertThat(orders.findLineBatch(lineId)).isEqualTo(first);

        assertThat(orders.recordLineBatch(lineId, second))
                .as("已记录批次的行不允许被覆盖，否则退货会回冲到错误批次")
                .isFalse();
        assertThat(orders.findLineBatch(lineId)).isEqualTo(first);
    }
}
