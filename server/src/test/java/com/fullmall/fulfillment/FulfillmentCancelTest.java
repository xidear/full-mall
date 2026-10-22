package com.fullmall.fulfillment;

import com.fullmall.fulfillment.app.FulfillmentService;
import com.fullmall.fulfillment.domain.FulfillmentStatus;
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

/** D6 履约取消：未拣货可取消并释放预占；已拣货/已发货拒绝。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class FulfillmentCancelTest {

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
    FulfillmentService fulfillments;

    @Autowired
    JdbcTemplate jdbc;

    private record Fixture(UUID orderId, UUID fulfillmentId, UUID batchId, UUID orderLineId) {
    }

    private Fixture fixture(String status, int reserved) {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID fulfillmentId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '取消测试', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1000, 'CNY')
                """, skuId, productId, "FC-" + skuId.toString().substring(0, 8));
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, 'p', '13800000000', 'SHIP')
                """, orderId, "FC-" + orderId.toString().substring(0, 8));
        jdbc.update("""
                INSERT INTO order_line (id, order_id, sku_id, title_snapshot, sku_code_snapshot,
                                        unit_price_amount, currency, fulfillment_type, quantity)
                VALUES (?, ?, ?, '取消测试', 'FC-SKU', 1000, 'CNY', 'WAREHOUSE_SHIP', 1)
                """, lineId, orderId, skuId);
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no, quantity, reserved_quantity)
                VALUES (?, ?, 'WAREHOUSE', ?, ?, 5, ?)
                """, batchId, skuId, UUID.randomUUID(), "FC-B", reserved);
        jdbc.update("""
                INSERT INTO fulfillment_order (id, order_id, type, status)
                VALUES (?, ?, 'WAREHOUSE_SHIP', ?)
                """, fulfillmentId, orderId, status);
        if (reserved > 0) {
            jdbc.update("""
                    INSERT INTO reservation (id, order_id, order_line_id, batch_id, quantity, status)
                    VALUES (?, ?, ?, ?, ?, 'RESERVED')
                    """, UUID.randomUUID(), orderId, lineId, batchId, reserved);
        }
        return new Fixture(orderId, fulfillmentId, batchId, lineId);
    }

    @Test
    void cancelAllocatedReleasesReservation() {
        Fixture f = fixture("ALLOCATED", 1);

        var cancelled = fulfillments.cancelFulfillment(f.fulfillmentId(), "顾客改主意");

        assertThat(cancelled.status()).isEqualTo(FulfillmentStatus.CANCELLED);
        assertThat(jdbc.queryForObject(
                "SELECT reserved_quantity FROM inventory_batch WHERE id = ?", Integer.class, f.batchId()))
                .as("预占必须释放").isZero();
    }

    @Test
    void cancelIsRejectedAfterPickingOrShipping() {
        Fixture picking = fixture("PICKING", 1);
        assertThatThrownBy(() -> fulfillments.cancelFulfillment(picking.fulfillmentId(), "x"))
                .hasMessageContaining("已开始拣货");

        Fixture shipped = fixture("SHIPPED", 1);
        assertThatThrownBy(() -> fulfillments.cancelFulfillment(shipped.fulfillmentId(), "x"))
                .hasMessageContaining("请走售后");
    }
}
