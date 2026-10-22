package com.fullmall.aftersale;

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

/** 退货质检：只在 RECEIVED 后可记、只能记一次、非法结论拒绝。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class QualityInspectionTest {

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
    com.fullmall.aftersale.app.AfterSaleService afterSales;

    @Autowired
    JdbcTemplate jdbc;

    private UUID afterSale(String status) {
        UUID orderId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'FULFILLED', 'CNY', 1000, 0, 0, 1000, 'p', '13800000000', 'SHIP')
                """, orderId, "QT-" + orderId.toString().substring(0, 8));
        jdbc.update("""
                INSERT INTO after_sale (id, after_sale_no, order_id, type, status, reason, contact_phone,
                                        refund_amount, currency)
                VALUES (?, ?, ?, 'RETURN', ?, '质量问题', '13800000000', 1000, 'CNY')
                """, id, "AS-" + id.toString().substring(0, 8), orderId, status);
        return id;
    }

    @Test
    void qualityCanBeRecordedOnceAfterReceipt() {
        UUID id = afterSale("RECEIVED");

        afterSales.recordQuality(id, "pass", "包装完好", "op");
        assertThat(jdbc.queryForObject("SELECT quality_result FROM after_sale WHERE id = ?",
                String.class, id)).isEqualTo("PASS");

        assertThatThrownBy(() -> afterSales.recordQuality(id, "FAIL", "重复", "op"))
                .hasMessageContaining("已记录质检结论");
    }

    @Test
    void qualityRequiresReceivedStatusAndValidResult() {
        UUID approved = afterSale("APPROVED");
        assertThatThrownBy(() -> afterSales.recordQuality(approved, "PASS", null, "op"))
                .hasMessageContaining("只有已收货");

        UUID received = afterSale("RECEIVED");
        assertThatThrownBy(() -> afterSales.recordQuality(received, "MAYBE", null, "op"))
                .hasMessageContaining("只能是 PASS 或 FAIL");
    }
}
