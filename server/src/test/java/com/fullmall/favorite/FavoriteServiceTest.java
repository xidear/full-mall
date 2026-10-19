package com.fullmall.favorite;

import com.fullmall.favorite.app.FavoriteService;
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
class FavoriteServiceTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final UUID SKU = UUID.fromString("ffffffff-1111-2222-3333-444444444444");
    private static final UUID PRODUCT = UUID.fromString("ffffffff-0000-0000-0000-000000000001");
    private static final UUID MEMBER = UUID.fromString("ffffffff-9999-9999-9999-999999999999");

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

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM member_favorite");
        jdbc.update("DELETE FROM member WHERE id = ?", MEMBER);
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, 'M-FAV-1', '收藏用户', '13944440001', 'STANDARD', 0, 0, 'CNY')
                """, MEMBER);
        jdbc.update("""
                INSERT INTO product (id, title, status) VALUES (?, '收藏测试商品', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
                """, PRODUCT);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, 'SKU-FAV', 1000, 'CNY') ON CONFLICT (id) DO NOTHING
                """, SKU, PRODUCT);
    }

    @Test
    void addIsIdempotentAndListed() {
        assertThat(favorites.add(MEMBER, SKU)).isTrue();
        assertThat(favorites.add(MEMBER, SKU)).isFalse();

        assertThat(favorites.list(MEMBER, 10)).hasSize(1)
                .allSatisfy(favorite -> assertThat(favorite.skuId()).isEqualTo(SKU));
    }

    @Test
    void removeIsIdempotent() {
        favorites.add(MEMBER, SKU);

        assertThat(favorites.remove(MEMBER, SKU)).isTrue();
        assertThat(favorites.remove(MEMBER, SKU)).isFalse();
        assertThat(favorites.list(MEMBER, 10)).isEmpty();
    }

    @Test
    void unknownSkuIsRejected() {
        assertThatThrownBy(() -> favorites.add(MEMBER, UUID.randomUUID()))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("商品不存在");
    }
}
