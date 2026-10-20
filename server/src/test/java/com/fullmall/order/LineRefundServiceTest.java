package com.fullmall.order;

import com.fullmall.order.app.LineRefundService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
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
class LineRefundServiceTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final UUID PRODUCT = UUID.fromString("abababab-0000-0000-0000-000000000001");
    private static final UUID SKU_A = UUID.fromString("abababab-0000-0000-0000-000000000002");
    private static final UUID SKU_B = UUID.fromString("abababab-0000-0000-0000-000000000003");

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
    LineRefundService refunds;

    @Autowired
    JdbcTemplate jdbc;

    private UUID orderId;
    private UUID lineA;
    private UUID lineB;

    @BeforeEach
    void seed() {
        UUID existingOrder = UUID.randomUUID();
        jdbc.update("DELETE FROM order_line_refund WHERE order_id IN (SELECT id FROM orders WHERE order_no LIKE 'LR-%')");
        jdbc.update("DELETE FROM order_line WHERE order_id IN (SELECT id FROM orders WHERE order_no LIKE 'LR-%')");
        jdbc.update("DELETE FROM orders WHERE order_no LIKE 'LR-%'");

        jdbc.update("""
                INSERT INTO product (id, title, status) VALUES (?, '行退款测试商品', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
                """, PRODUCT);
        String[][] skus = {{SKU_A.toString(), "SKU-LR-A"}, {SKU_B.toString(), "SKU-LR-B"}};
        for (String[] sku : skus) {
            jdbc.update("""
                    INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                    VALUES (?, ?, ?, 1000, 'CNY')
                    ON CONFLICT (id) DO UPDATE SET sku_code = EXCLUDED.sku_code
                    """, UUID.fromString(sku[0]), PRODUCT, sku[1]);
        }

        orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 3000, 0, 0, 3000, '行退款', '13900001234', 'SHIP')
                """, orderId, "LR-" + orderId);
        lineA = insertLine(SKU_A, 2);
        lineB = insertLine(SKU_B, 1);
    }

    private UUID insertLine(UUID skuId, int quantity) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO order_line (id, order_id, sku_id, title_snapshot, sku_code_snapshot,
                                        quantity, unit_price_amount, currency, fulfillment_type)
                VALUES (?, ?, ?, '行退款测试商品', 'SKU-LR', ?, 1000, 'CNY', 'WAREHOUSE_SHIP')
                """, id, orderId, skuId, quantity);
        return id;
    }

    @Test
    void recordsPartialRefundPerLine() {
        var refunded = refunds.record(orderId, lineA, 1, 1000, "CNY", "破损");

        assertThat(refunded.quantity()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount_amount),0) FROM order_line_refund WHERE order_line_id = ?",
                Long.class, lineA)).isEqualTo(1000);
    }

    @Test
    void cannotRefundMoreThanLineQuantity() {
        refunds.record(orderId, lineA, 2, 2000, "CNY", null);

        assertThatThrownBy(() -> refunds.record(orderId, lineA, 1, 1000, "CNY", null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("超过该行数量");
    }

    @Test
    void cannotRefundMoreThanLineAmount() {
        assertThatThrownBy(() -> refunds.record(orderId, lineB, 1, 1001, "CNY", null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("超过该行金额");
    }

    @Test
    void rejectsLineFromAnotherOrder() {
        assertThatThrownBy(() -> refunds.record(UUID.randomUUID(), lineA, 1, 100, "CNY", null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不属于此订单");
    }

    @Test
    void allocationSumsExactlyToRefundAmount() {
        // 剩余：A 行 2000、B 行 1000，共 3000；退 1000 → 按 2:1 分摊 667 / 333
        var allocation = refunds.allocate(orderId, 1000, "CNY");

        assertThat(allocation).hasSize(2);
        // 关键性质：分摊之和**恰好**等于退款额（不丢分、不多出）
        assertThat(allocation.stream().mapToLong(LineRefundService.Refunded::amount).sum())
                .isEqualTo(1000);
        // 2:1 分成 666 / 334（尾差给最后一个有剩余的行），顺序依行 id
        assertThat(allocation.stream().map(LineRefundService.Refunded::amount))
                .containsExactlyInAnyOrder(666L, 334L);
    }

    @Test
    void allocationRejectsAmountBeyondRemaining() {
        assertThatThrownBy(() -> refunds.allocate(orderId, 3001, "CNY"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("超过剩余可退");
    }
}
