package com.fullmall.catalog;

import com.fullmall.catalog.app.SerialNumberService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 序列号：登记成功、超批次拒绝、重复拒绝、按号可查。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class SerialNumberServiceTest {

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
    SerialNumberService serials;

    @Autowired
    JdbcTemplate jdbc;

    private UUID batch(int quantity, String tag) {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '手机', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 599900, 'CNY')
                """, skuId, productId, tag + "-" + skuId.toString().substring(0, 6));
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no, quantity)
                VALUES (?, ?, 'WAREHOUSE', ?, ?, ?)
                """, batchId, skuId, UUID.randomUUID(), tag + "-B", quantity);
        return batchId;
    }

    @Test
    void registersAndLooksUpSerial() {
        UUID batchId = batch(5, "SN1");

        var result = serials.register(batchId, List.of("imei-001", "IMEI-002"));

        assertThat(result.created()).isEqualTo(2);
        var found = serials.lookup("imei-001");
        assertThat(found.get("status")).isEqualTo("IN_STOCK");
        assertThat(found.get("batchId")).isEqualTo(batchId);
        assertThat(serials.lookup("IMEI-002").get("serialNo")).isEqualTo("IMEI-002");
    }

    @Test
    void rejectsOverBatchDuplicateAndUnknownSerial() {
        UUID batchId = batch(2, "SN2");
        serials.register(batchId, List.of("S1", "S2"));

        assertThatThrownBy(() -> serials.register(batchId, List.of("S3")))
                .hasMessageContaining("会超出");
        assertThatThrownBy(() -> serials.register(batchId, List.of("S1")))
                .hasMessageContaining("已登记");
        assertThatThrownBy(() -> serials.lookup("NOPE"))
                .hasMessageContaining("序列号不存在");
        assertThatThrownBy(() -> serials.register(batchId, List.of("X", "X")))
                .hasMessageContaining("有重复");
    }

    private UUID orderLine(UUID skuId) {
        UUID orderId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 599900, 0, 0, 599900, 'p', '13800000000', 'SHIP')
                """, orderId, "SN-" + orderId.toString().substring(0, 8));
        jdbc.update("""
                INSERT INTO order_line (id, order_id, sku_id, title_snapshot, sku_code_snapshot,
                                        unit_price_amount, currency, fulfillment_type, quantity)
                VALUES (?, ?, ?, '手机', 'SN-SKU', 599900, 'CNY', 'SHIP', 1)
                """, lineId, orderId, skuId);
        return lineId;
    }

    private UUID skuOfBatch(UUID batchId) {
        return jdbc.queryForObject("SELECT sku_id FROM inventory_batch WHERE id = ?",
                UUID.class, batchId);
    }

    @Test
    void bindsSerialsToOrderLineAndRejectsReuse() {
        UUID batchId = batch(3, "SN3");
        serials.register(batchId, List.of("B1", "B2"));
        UUID lineId = orderLine(skuOfBatch(batchId));

        int bound = serials.bindToOrderLine(lineId, List.of("B1", "B2"));

        assertThat(bound).isEqualTo(2);
        assertThat(serials.lookup("B1").get("status")).isEqualTo("SOLD");
        assertThat(serials.lookup("B1").get("orderLineId")).isEqualTo(lineId);
        assertThatThrownBy(() -> serials.bindToOrderLine(lineId, List.of("B1")))
                .hasMessageContaining("不能出库");
    }

    @Test
    void rejectsSerialFromAnotherSku() {
        UUID batchA = batch(2, "SN4");
        UUID batchB = batch(2, "SN5");
        serials.register(batchB, List.of("OTHER-1"));
        UUID lineForA = orderLine(skuOfBatch(batchA));

        assertThatThrownBy(() -> serials.bindToOrderLine(lineForA, List.of("OTHER-1")))
                .hasMessageContaining("不属于该订单行");
    }
}
