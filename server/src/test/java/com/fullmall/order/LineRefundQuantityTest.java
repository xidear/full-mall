package com.fullmall.order;

import com.fullmall.order.app.LineRefundService;
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

/** 金额分摊不写数量：同一行多次部分退款不会撞"数量超限"。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class LineRefundQuantityTest {

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
    LineRefundService lineRefunds;

    @Autowired
    JdbcTemplate jdbc;

    private UUID order(int quantity, long unitPrice) {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', ?, 0, 0, ?, 'p', '13900006666', 'SHIP')
                """, orderId, "LR-" + orderId.toString().substring(0, 8),
                quantity * unitPrice, quantity * unitPrice);
        jdbc.update("""
                INSERT INTO order_line (id, order_id, sku_id, title_snapshot, sku_code_snapshot, quantity,
                                        unit_price_amount, currency, fulfillment_type, line_type)
                VALUES (?, ?, ?, '商品', 'SKU', ?, ?, 'CNY', 'WAREHOUSE_SHIP', 'NORMAL')
                """, UUID.randomUUID(), orderId, UUID.randomUUID(), quantity, unitPrice);
        return orderId;
    }

    @Test
    void repeatedPartialRefundsDoNotExceedQuantity() {
        UUID orderId = order(1, 10_000);   // 单件 100 元

        lineRefunds.allocateAndRecord(orderId, 4_000, "CNY");
        lineRefunds.allocateAndRecord(orderId, 6_000, "CNY");

        var total = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount_amount),0) FROM order_line_refund WHERE order_id = ?",
                Long.class, orderId);
        assertThat(total).isEqualTo(10_000);

        // 数量列：金额无法整除单价，应为 NULL（不瞎填）
        var quantities = jdbc.queryForList(
                "SELECT quantity FROM order_line_refund WHERE order_id = ?", Integer.class, orderId);
        assertThat(quantities).allSatisfy(q -> assertThat(q).isNull());
    }

    @Test
    void wholeUnitRefundRecordsQuantity() {
        UUID orderId = order(2, 5_000);    // 两件，每件 50 元

        lineRefunds.allocateAndRecord(orderId, 5_000, "CNY");   // 整好一件

        var quantities = jdbc.queryForList(
                "SELECT quantity FROM order_line_refund WHERE order_id = ?", Integer.class, orderId);
        assertThat(quantities).containsExactly(1);
    }
}
