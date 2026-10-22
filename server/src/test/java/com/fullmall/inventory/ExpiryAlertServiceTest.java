package com.fullmall.inventory;

import com.fullmall.inventory.app.ExpiryAlertService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 临期预警：只列 N 天内到期且有量的批次；已过期 daysLeft 为负。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ExpiryAlertServiceTest {

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
    ExpiryAlertService alerts;

    @Autowired
    JdbcTemplate jdbc;

    private UUID sku(String code) {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '临期品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1000, 'CNY')
                """, skuId, productId, code);
        return skuId;
    }

    private void batch(UUID skuId, String no, LocalDate expiry, int qty) {
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no, quantity,
                                             expiry_date, status)
                VALUES (?, ?, 'WAREHOUSE', ?, ?, ?, ?, 'AVAILABLE')
                """, UUID.randomUUID(), skuId, UUID.randomUUID(), no, qty, expiry);
    }

    @Test
    void listsOnlyBatchesWithinWindowWithStock() {
        UUID sku = sku("EXP-" + UUID.randomUUID().toString().substring(0, 6));
        batch(sku, "SOON", LocalDate.now().plusDays(3), 5);
        batch(sku, "EXPIRED", LocalDate.now().minusDays(2), 4);
        batch(sku, "LATER", LocalDate.now().plusDays(90), 6);
        batch(sku, "EMPTY", LocalDate.now().plusDays(2), 0);

        var rows = alerts.expiringBatches(50);

        assertThat(rows).extracting(row -> row.get("batchNo"))
                .contains("SOON", "EXPIRED")
                .doesNotContain("LATER", "EMPTY");
        assertThat(rows).anySatisfy(row -> {
            if ("EXPIRED".equals(row.get("batchNo"))) {
                assertThat(((Number) row.get("daysLeft")).longValue()).isNegative();
            }
        });
    }
}
