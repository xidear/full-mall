package com.fullmall.purchase;

import com.fullmall.catalog.app.SkuCostService;
import com.fullmall.purchase.app.PurchaseService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 收货产生成本：两次采购不同单价 -> 加权均价；重复收货被拒。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class PurchaseReceiptCostTest {

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
    PurchaseService purchases;

    @Autowired
    SkuCostService costs;

    @Autowired
    JdbcTemplate jdbc;

    private UUID sku() {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '采购商品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 3000, 'CNY')
                """, skuId, productId, "PO-" + skuId.toString().substring(0, 8));
        return skuId;
    }

    @Test
    void receivingTwoPurchasesProducesWeightedAverage() {
        UUID skuId = sku();

        UUID first = purchases.create(null, "CNY", List.of(new PurchaseService.Line(skuId, 10, 800)));
        purchases.receive(first);
        UUID second = purchases.create(null, "CNY", List.of(new PurchaseService.Line(skuId, 10, 1_200)));
        purchases.receive(second);

        var cost = costs.current(skuId);
        assertThat(cost.quantity()).isEqualTo(20);
        assertThat(cost.amount()).isEqualTo(20_000);
        assertThat(cost.avgCost()).isEqualTo(1_000);
    }

    @Test
    void receivingTwiceIsRejected() {
        UUID skuId = sku();
        UUID id = purchases.create(null, "CNY", List.of(new PurchaseService.Line(skuId, 1, 500)));
        purchases.receive(id);

        assertThatThrownBy(() -> purchases.receive(id))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("只有已下单");
    }
}
