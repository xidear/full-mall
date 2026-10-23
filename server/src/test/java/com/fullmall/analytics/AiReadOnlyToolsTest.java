package com.fullmall.analytics;

import com.fullmall.analytics.tool.AiReadOnlyTools;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** AI 只读工具：白名单、参数校验、只读查询结果正确。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class AiReadOnlyToolsTest {

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
    AiReadOnlyTools tools;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void readsOrderSkuAvailabilityAndPrice() {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String orderNo = "AI-" + orderId.toString().substring(0, 8);
        String skuCode = "AISKU-" + skuId.toString().substring(0, 6);
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, 'AI 测试', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1990, 'CNY')
                """, skuId, productId, skuCode);
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no, quantity, reserved_quantity)
                VALUES (?, ?, 'WAREHOUSE', ?, 'AI-B', 7, 2)
                """, UUID.randomUUID(), skuId, UUID.randomUUID());
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 1990, 0, 0, 1990, 'p', '13800000000', 'SHIP')
                """, orderId, orderNo);

        assertThat(tools.invoke("order.status", Map.of("orderNo", orderNo)))
                .containsEntry("status", "PAID");
        assertThat(tools.invoke("sku.price", Map.of("skuCode", skuCode)))
                .containsEntry("priceAmount", 1990L);
        assertThat(tools.invoke("sku.availability", Map.of("skuCode", skuCode)))
                .containsEntry("available", 5L);
    }

    @Test
    void rejectsUnknownToolAndMissingArgs() {
        assertThatThrownBy(() -> tools.invoke("order.delete", Map.of()))
                .hasMessageContaining("未登记的工具");
        assertThatThrownBy(() -> tools.invoke("order.status", Map.of()))
                .hasMessageContaining("缺少参数");
        assertThatThrownBy(() -> tools.invoke("sku.price", Map.of("skuCode", "NOPE")))
                .hasMessageContaining("SKU 不存在");
    }
}
