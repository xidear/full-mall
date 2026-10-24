package com.fullmall.catalog;

import com.fullmall.catalog.app.ProductMediaService;
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

/** 商品素材：多图/视频/规格图；主图唯一；规格图必须绑 SKU。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ProductMediaTest {

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
    ProductMediaService media;

    @Autowired
    JdbcTemplate jdbc;

    private UUID product() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '素材商品', 'ACTIVE')", id);
        return id;
    }

    private UUID sku(UUID productId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1000, 'CNY')
                """, id, productId, "PM-" + id.toString().substring(0, 6));
        return id;
    }

    private UUID asset(String url) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO asset (id, name, url, kind, owner_username, purpose)
                VALUES (?, '素材', ?, 'IMAGE', 'admin', 'PRODUCT')
                """, id, url);
        return id;
    }

    @Test
    void attachesGalleryVideoAndSpecButOnlyOneMain() {
        UUID productId = product();
        UUID skuId = sku(productId);
        UUID main = asset("https://cdn.example.com/main.png");
        UUID gallery = asset("https://cdn.example.com/g1.png");
        UUID spec = asset("https://cdn.example.com/spec.png");

        media.attach(productId, null, main, "MAIN", 0, "AI", "admin");
        media.attach(productId, null, gallery, "GALLERY", 1, "UPLOAD", "admin");
        media.attach(productId, skuId, spec, "SPEC", 0, "UPLOAD", "admin");

        assertThat(media.list(productId)).hasSize(3);

        UUID otherMain = asset("https://cdn.example.com/main2.png");
        assertThatThrownBy(() -> media.attach(productId, null, otherMain, "MAIN", 0, "UPLOAD", "admin"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void rejectsSpecWithoutSkuAndMainWithSku() {
        UUID productId = product();
        UUID skuId = sku(productId);
        UUID assetId = asset("https://cdn.example.com/x.png");

        assertThatThrownBy(() -> media.attach(productId, null, assetId, "SPEC", 0, "UPLOAD", "admin"))
                .hasMessageContaining("规格专用图必须指定 skuId");
        assertThatThrownBy(() -> media.attach(productId, skuId, assetId, "MAIN", 0, "UPLOAD", "admin"))
                .hasMessageContaining("主图是商品级");
    }
}
