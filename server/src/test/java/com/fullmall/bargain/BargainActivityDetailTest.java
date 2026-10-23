package com.fullmall.bargain;

import com.fullmall.bargain.app.BargainService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 活动详情：返回标题/原价/底价/人数；不存在时明确 404。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class BargainActivityDetailTest {

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
    BargainService bargain;

    @Autowired
    JdbcTemplate jdbc;

    private UUID activity() {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '砍价商品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 9990, 'CNY')
                """, skuId, productId, "BG-" + skuId.toString().substring(0, 6));
        jdbc.update("""
                INSERT INTO bargain_activity (id, sku_id, floor_price, max_cut, min_helpers,
                                              cut_min, cut_max, currency, expires_hours, status,
                                              starts_at, ends_at)
                VALUES (?, ?, 100, 9890, 3, 50, 100, 'CNY', 24, 'ACTIVE',
                        now() - interval '1 hour', now() + interval '1 day')
                """, activityId, skuId);
        return activityId;
    }

    @Test
    void returnsDetailAndRejectsMissing() {
        UUID id = activity();

        var detail = bargain.activityDetail(id);

        assertThat(detail).containsEntry("title", "砍价商品")
                .containsEntry("listPrice", 9990L)
                .containsEntry("floorPrice", 100L)
                .containsEntry("minHelpers", 3);

        assertThatThrownBy(() -> bargain.activityDetail(UUID.randomUUID()))
                .hasMessageContaining("砍价活动不存在");
    }
}
