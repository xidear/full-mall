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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 盘点审批：大差异挂起且**不调账**；通过后才调账并写流水；驳回后库存不变。
 * 阈值固定为 5 作测试配置。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.stocktake.approval-threshold=5"
        })
@ActiveProfiles("test")
class StocktakeApprovalTest {

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

    private UUID stocktakeWithVariance(int counted) {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '盘点品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1000, 'CNY')
                """, skuId, productId, "ST-" + skuId.toString().substring(0, 8));
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no, quantity)
                VALUES (?, ?, 'WAREHOUSE', ?, ?, 10)
                """, batchId, skuId, locationId, "B-" + batchId.toString().substring(0, 8));

        UUID stocktakeId = warehouse.createStocktake(
                new com.fullmall.warehouse.api.WarehouseDtos.CreateStocktakeRequest(
                        "WAREHOUSE", locationId, "测试盘点")).id();
        var view = warehouse.findStocktake(stocktakeId);
        UUID lineId = view.lines().get(0).id();
        warehouse.confirmStocktake(stocktakeId,
                new com.fullmall.warehouse.api.WarehouseDtos.ConfirmStocktakeRequest(
                        List.of(new com.fullmall.warehouse.api.WarehouseDtos.ConfirmStocktakeRequest.CountedLine(
                                lineId, counted, "测试"))));
        return stocktakeId;
    }

    private int batchQuantity(UUID stocktakeId) {
        return jdbc.queryForObject("""
                SELECT b.quantity FROM inventory_batch b
                 JOIN stocktake_line l ON l.batch_id = b.id
                 WHERE l.stocktake_id = ?
                """, Integer.class, stocktakeId);
    }

    @org.junit.jupiter.api.BeforeEach
    void authenticate() {
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "tester", "n/a", java.util.List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
    }

    @org.junit.jupiter.api.AfterEach
    void clear() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void smallVarianceAppliesImmediately() {
        UUID id = stocktakeWithVariance(12);   // 差异 +2，阈值 5 内

        assertThat(warehouse.findStocktake(id).approvalStatus()).isEqualTo("NONE");
        assertThat(batchQuantity(id)).isEqualTo(12);
    }

    @Test
    void largeVarianceWaitsForApprovalAndAppliesAfterApprove() {
        UUID id = stocktakeWithVariance(30);   // 差异 +20

        assertThat(warehouse.findStocktake(id).approvalStatus()).isEqualTo("PENDING");
        assertThat(batchQuantity(id)).as("未经审批不得改库存").isEqualTo(10);

        warehouse.approveStocktake(id);

        assertThat(warehouse.findStocktake(id).approvalStatus()).isEqualTo("APPROVED");
        assertThat(batchQuantity(id)).isEqualTo(30);
    }

    @Test
    void rejectedVarianceKeepsStockUnchanged() {
        UUID id = stocktakeWithVariance(30);
        warehouse.rejectStocktake(id);

        assertThat(warehouse.findStocktake(id).approvalStatus()).isEqualTo("REJECTED");
        assertThat(batchQuantity(id)).isEqualTo(10);
    }
}
