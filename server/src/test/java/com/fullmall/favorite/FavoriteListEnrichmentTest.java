package com.fullmall.favorite;

import com.fullmall.favorite.app.FavoriteService;
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

/** 收藏列表要带商品标题与价格，否则顾客端只能显示一串 skuId。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class FavoriteListEnrichmentTest {

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
    FavoriteService favorites;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void listIncludesProductTitleAndPrice() {
        UUID memberId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance,
                                    stored_value_balance, currency)
                VALUES (?, ?, '收藏用户', ?, 'STANDARD', 0, 0, 'CNY')
                """, memberId, "M-ENR-" + memberId.toString().substring(0, 8),
                "138" + memberId.toString().replaceAll("[^0-9]", "2").substring(0, 8));
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, ?, 'ACTIVE')", productId, "收藏商品");
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 2599, 'CNY')
                """, skuId, productId, "SKU-ENR-" + skuId.toString().substring(0, 6));

        favorites.add(memberId, skuId);

        var list = favorites.list(memberId, 10);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).title()).isEqualTo("收藏商品");
        assertThat(list.get(0).priceAmount()).isEqualTo(2599);
        assertThat(list.get(0).productId()).isEqualTo(productId);
    }
}
