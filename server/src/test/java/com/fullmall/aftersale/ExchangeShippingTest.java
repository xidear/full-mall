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
