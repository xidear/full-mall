package com.fullmall.cart.app;

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
class CartServiceTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final UUID SKU_A = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SKU_B = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

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
    CartService carts;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void seedSkus() {
        jdbc.update("DELETE FROM cart_item");
        jdbc.update("DELETE FROM cart");
        UUID productId = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
        jdbc.update("""
                INSERT INTO product (id, title, status) VALUES (?, '测试商品', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
                """, productId);
        for (UUID skuId : java.util.List.of(SKU_A, SKU_B)) {
            jdbc.update("""
                    INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                    VALUES (?, ?, ?, 1000, 'CNY')
                    ON CONFLICT (id) DO NOTHING
                    """, skuId, productId, "SKU-" + skuId.toString().substring(0, 8));
        }
    }

    @Test
    void addingSameSkuAccumulatesQuantity() {
        carts.addItem("guest:t1", SKU_A, 2);
        carts.addItem("guest:t1", SKU_A, 3);

        assertThat(carts.items("guest:t1")).hasSize(1)
                .allSatisfy(item -> assertThat(item.quantity()).isEqualTo(5));
    }

    @Test
    void zeroQuantityIsRejectedInsteadOfSilentRemoval() {
        assertThatThrownBy(() -> carts.addItem("guest:t2", SKU_A, 0))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("数量");
    }

    @Test
    void mergeAddsGuestItemsToMemberCartAndClearsGuest() {
        carts.addItem("guest:t3", SKU_A, 1);
        carts.addItem("member:m1", SKU_A, 4);
        carts.addItem("member:m1", SKU_B, 2);

        assertThat(carts.merge("guest:t3", "member:m1")).isEqualTo(1);

        assertThat(carts.items("member:m1")).hasSize(2);
        assertThat(carts.items("member:m1")).anySatisfy(item -> {
            if (item.skuId().equals(SKU_A)) {
                assertThat(item.quantity()).isEqualTo(5);
            }
        });
        assertThat(carts.items("guest:t3")).isEmpty();
    }

    @Test
    void removingMissingItemIsNotFound() {
        carts.addItem("guest:t4", SKU_A, 1);
        carts.remove("guest:t4", SKU_A);

        assertThatThrownBy(() -> carts.remove("guest:t4", SKU_A))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("没有该商品");
    }
}
