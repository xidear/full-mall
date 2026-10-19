package com.fullmall.history;

import com.fullmall.history.app.BrowseHistoryService;
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
class BrowseHistoryServiceTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final UUID SKU_A = UUID.fromString("ffffffff-aaaa-0000-0000-000000000001");
    private static final UUID SKU_B = UUID.fromString("ffffffff-bbbb-0000-0000-000000000002");
    private static final UUID PRODUCT = UUID.fromString("ffffffff-0000-0000-0000-000000000009");
    private static final UUID MEMBER = UUID.fromString("ffffffff-9999-0000-0000-000000000009");

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

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM member_browse_history");
        jdbc.update("DELETE FROM member WHERE id = ?", MEMBER);
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, 'M-HIS-1', '足迹用户', '13955550001', 'STANDARD', 0, 0, 'CNY')
                """, MEMBER);
        jdbc.update("""
                INSERT INTO product (id, title, status) VALUES (?, '足迹测试商品', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
                """, PRODUCT);
        for (UUID sku : new UUID[]{SKU_A, SKU_B}) {
            jdbc.update("""
                    INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                    VALUES (?, ?, ?, 1000, 'CNY') ON CONFLICT (id) DO NOTHING
                    """, sku, PRODUCT, "SKU-HIS-" + sku.toString().substring(9, 13));
        }
    }

    @Test
    void repeatedViewUpdatesRecencyInsteadOfDuplicating() {
        history.record(MEMBER, SKU_A);
        history.record(MEMBER, SKU_B);
        history.record(MEMBER, SKU_A); // 再看一次 A，应排到最前

        var entries = history.recent(MEMBER, 10);

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).skuId()).isEqualTo(SKU_A);
        assertThat(entries.get(0).viewCount()).isEqualTo(2);
    }

    @Test
    void clearRemovesAllEntriesForMember() {
        history.record(MEMBER, SKU_A);
        history.record(MEMBER, SKU_B);

        assertThat(history.clear(MEMBER)).isEqualTo(2);
        assertThat(history.recent(MEMBER, 10)).isEmpty();
    }

    @Test
    void unknownSkuIsRejected() {
        assertThatThrownBy(() -> history.record(MEMBER, UUID.randomUUID()))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("商品不存在");
    }
}
