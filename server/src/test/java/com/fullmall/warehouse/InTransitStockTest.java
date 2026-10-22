package com.fullmall.warehouse;

import com.fullmall.warehouse.app.WarehouseService;
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

/** 在途库存：只统计 IN_TRANSIT，接收后不再出现在列表。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class InTransitStockTest {

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
    WarehouseService warehouse;

    @Autowired
    JdbcTemplate jdbc;

    private UUID transfer(String status, int quantity) {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '在途品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1000, 'CNY')
                """, skuId, productId, "IT-" + skuId.toString().substring(0, 8));
        jdbc.update("""
                INSERT INTO stock_transfer (id, transfer_no, from_type, from_id, to_type, to_id,
                                            status, shipped_at)
                VALUES (?, ?, 'WAREHOUSE', ?, 'STORE', ?, ?, now())
                """, transferId, "TR-" + transferId.toString().substring(0, 8),
                UUID.randomUUID(), UUID.randomUUID(), status);
        jdbc.update("""
                INSERT INTO stock_transfer_line (id, transfer_id, sku_id, quantity)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), transferId, skuId, quantity);
        return transferId;
    }

    @Test
    void onlyInTransitTransfersAreListed() {
        UUID inTransit = transfer("IN_TRANSIT", 7);
        transfer("RECEIVED", 9);

        var rows = warehouse.inTransit(50);

        assertThat(rows).anySatisfy(row -> {
            assertThat(row.get("transferNo")).isEqualTo(
                    jdbc.queryForObject("SELECT transfer_no FROM stock_transfer WHERE id = ?",
                            String.class, inTransit));
            assertThat(((Number) row.get("quantity")).intValue()).isEqualTo(7);
        });
        assertThat(rows).allSatisfy(row ->
                assertThat(((Number) row.get("quantity")).intValue()).isNotEqualTo(9));
    }
}
