package com.fullmall.aftersale;

import com.fullmall.aftersale.app.AfterSaleService;
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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ExchangeShippingTest {

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
    AfterSaleService afterSales;

    @Autowired
    JdbcTemplate jdbc;

    private UUID seedAfterSale(String type, String status, String phone) {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, '换货用户', ?, 'SHIP')
                """, orderId, "EX-" + orderId, phone);
        UUID afterSaleId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO after_sale (id, after_sale_no, order_id, type, status, reason,
                                        contact_phone, refund_amount, currency)
                VALUES (?, ?, ?, ?, ?, '质量问题', ?, 0, 'CNY')
                """, afterSaleId, "AS-" + afterSaleId, orderId, type, status, phone);
        return afterSaleId;
    }

    @Test
    void exchangeShippingStoresTrackingAndNotifies() {
        String phone = "1390000" + (1000 + Math.abs(UUID.randomUUID().hashCode() % 9000));
        UUID afterSaleId = seedAfterSale("EXCHANGE", "RECEIVED", phone);

        var view = afterSales.shipExchange(afterSaleId, "SF", "SF987654321");

        assertThat(view.status()).isEqualTo("EXCHANGE_SHIPPED");
        // 读接口必须能看到刚写入的物流信息（否则页面拿不到）
        assertThat(view.exchangeCarrierCode()).isEqualTo("SF");
        assertThat(view.exchangeTrackingNo()).isEqualTo("SF987654321");
        assertThat(view.exchangeShippedAt()).isNotNull();
        assertThat(jdbc.queryForObject(
                "SELECT exchange_tracking_no FROM after_sale WHERE id = ?", String.class, afterSaleId))
                .isEqualTo("SF987654321");
        assertThat(jdbc.queryForList("""
                SELECT channel FROM notification
                 WHERE recipient_type = 'CUSTOMER' AND recipient_id = ?
                   AND event_type = 'AFTER_SALE_EXCHANGE_SHIPPED'
                """, String.class, phone)).isNotEmpty();
    }

    private UUID batchId;

    @Test
    void exchangeShippingDeductsStock() {
        String phone = "1390000" + (1000 + Math.abs(UUID.randomUUID().hashCode() % 9000));
        UUID afterSaleId = seedExchangeWithStock(phone);

        int before = jdbc.queryForObject(
                "SELECT quantity FROM inventory_batch WHERE id = ?", Integer.class, batchId);

        afterSales.shipExchange(afterSaleId, "SF", "SF-DEDUCT-1");

        int after = jdbc.queryForObject(
                "SELECT quantity FROM inventory_batch WHERE id = ?", Integer.class, batchId);
        assertThat(after).isEqualTo(before - 1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_move WHERE batch_id = ? AND move_type = 'SHIP'",
                Integer.class, batchId)).isEqualTo(1);
    }

    /** 造一条"有商品行 + 有仓库库存"的换货售后。 */
    private UUID seedExchangeWithStock(String phone) {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, '换货用户', ?, 'SHIP')
                """, orderId, "EX2-" + orderId, phone);

        jdbc.update("""
                INSERT INTO market (code, name, currency, default_locale, active)
                VALUES ('EX2', '换货市场', 'CNY', 'zh-CN', TRUE) ON CONFLICT (code) DO NOTHING
                """);
        String marketCode = "EX2";
        UUID warehouseId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO warehouse (id, market_code, name, type, active)
                VALUES (?, ?, '换货仓', 'SELF', TRUE)
                """, warehouseId, marketCode);
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '换货商品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1000, 'CNY')
                """, skuId, productId, "SKU-EX-" + skuId.toString().substring(0, 8));
        batchId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no,
                                            quantity, reserved_quantity, status)
                VALUES (?, ?, 'WAREHOUSE', ?, ?, 10, 0, 'AVAILABLE')
                """, batchId, skuId, warehouseId, "EX-B-" + batchId.toString().substring(0, 8));

        UUID orderLineId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO order_line (id, order_id, sku_id, title_snapshot, sku_code_snapshot,
                                        quantity, unit_price_amount, currency, fulfillment_type)
                VALUES (?, ?, ?, '换货商品', 'SKU-EX', 1, 1000, 'CNY', 'WAREHOUSE_SHIP')
                """, orderLineId, orderId, skuId);

        UUID afterSaleId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO after_sale (id, after_sale_no, order_id, type, status, reason,
                                        contact_phone, refund_amount, currency)
                VALUES (?, ?, ?, 'EXCHANGE', 'RECEIVED', '质量问题', ?, 0, 'CNY')
                """, afterSaleId, "AS-" + afterSaleId, orderId, phone);
        jdbc.update("""
                INSERT INTO after_sale_line (id, after_sale_id, order_line_id, sku_id, quantity)
                VALUES (?, ?, ?, ?, 1)
                """, UUID.randomUUID(), afterSaleId, orderLineId, skuId);
        return afterSaleId;
    }

    @Test
    void exchangeCanBeCompletedAfterShipping() {
        String phone = "1390000" + (1000 + Math.abs(UUID.randomUUID().hashCode() % 9000));
        UUID afterSaleId = seedAfterSale("EXCHANGE", "RECEIVED", phone);
        afterSales.shipExchange(afterSaleId, "SF", "SF-COMPLETE-1");

        var completed = afterSales.completeExchange(afterSaleId, "客服小李");

        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM after_sale_event WHERE after_sale_id = ?"
                        + " AND event_type = 'EXCHANGE_COMPLETED'", Integer.class, afterSaleId))
                .isEqualTo(1);
    }

    @Test
    void exchangeCannotCompleteBeforeShipping() {
        String phone = "1390000" + (1000 + Math.abs(UUID.randomUUID().hashCode() % 9000));
        UUID afterSaleId = seedAfterSale("EXCHANGE", "RECEIVED", phone);

        assertThatThrownBy(() -> afterSales.completeExchange(afterSaleId, "客服"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("尚未发出");
    }

    @Test
    void returnAfterSaleCannotShipExchange() {
        String phone = "1390000" + (1000 + Math.abs(UUID.randomUUID().hashCode() % 9000));
        UUID afterSaleId = seedAfterSale("RETURN", "RECEIVED", phone);

        assertThatThrownBy(() -> afterSales.shipExchange(afterSaleId, "SF", "X1"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("只有换货");
    }

    @Test
    void exchangeMustBeReceivedFirst() {
        String phone = "1390000" + (1000 + Math.abs(UUID.randomUUID().hashCode() % 9000));
        UUID afterSaleId = seedAfterSale("EXCHANGE", "APPROVED", phone);

        assertThatThrownBy(() -> afterSales.shipExchange(afterSaleId, "SF", "X1"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("需先收到");
    }

    @Test
    void carrierAndTrackingRequired() {
        String phone = "1390000" + (1000 + Math.abs(UUID.randomUUID().hashCode() % 9000));
        UUID afterSaleId = seedAfterSale("EXCHANGE", "RECEIVED", phone);

        assertThatThrownBy(() -> afterSales.shipExchange(afterSaleId, " ", "X1"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("承运商");
        assertThatThrownBy(() -> afterSales.shipExchange(afterSaleId, "SF", " "))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("运单号");
    }
}
