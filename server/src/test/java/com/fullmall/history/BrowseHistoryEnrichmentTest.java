package com.fullmall.history;

import com.fullmall.history.app.BrowseHistoryService;
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

/** 足迹列表要带标题与价格，否则页面只能显示 skuId。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class BrowseHistoryEnrichmentTest {

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
    BrowseHistoryService history;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void recentIncludesTitleAndPrice() {
        UUID memberId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance,
                                    stored_value_balance, currency)
                VALUES (?, ?, '足迹用户', ?, 'STANDARD', 0, 0, 'CNY')
                """, memberId, "M-HIS-" + memberId.toString().substring(0, 8),
                "139" + memberId.toString().replaceAll("[^0-9]", "4").substring(0, 8));
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, ?, 'ACTIVE')",
                productId, "足迹商品");
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 3999, 'CNY')
                """, skuId, productId, "SKU-HIS-" + skuId.toString().substring(0, 6));

        history.record(memberId, skuId);

        var entries = history.recent(memberId, 10);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).title()).isEqualTo("足迹商品");
        assertThat(entries.get(0).priceAmount()).isEqualTo(3999);
        assertThat(entries.get(0).productId()).isEqualTo(productId);
        assertThat(entries.get(0).viewCount()).isEqualTo(1);
    }
}
