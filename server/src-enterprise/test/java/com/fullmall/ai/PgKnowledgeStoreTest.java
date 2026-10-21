package com.fullmall.ai;

import com.fullmall.ai.app.KnowledgeChunk;
import com.fullmall.ai.app.PgKnowledgeStore;
import com.fullmall.ai.app.ProductKnowledgeSyncService;
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

/** 生产知识库：关键词召回 + 向量重排；商品改价后重建覆盖旧价格。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.ai.enabled=true",
                "app.ai.mode=prod"
        })
@ActiveProfiles("test")
class PgKnowledgeStoreTest {

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
    PgKnowledgeStore store;

    @Autowired
    ProductKnowledgeSyncService productSync;

    @Autowired
    JdbcTemplate jdbc;

    private UUID skuWithPrice(long price) {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product (id, title, description, status)
                VALUES (?, '演示商品', '演示描述', 'ACTIVE')
                """, productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, ?, 'CNY')
                """, skuId, productId, "SKU-" + skuId.toString().substring(0, 8), price);
        return skuId;
    }

    @Test
    void productSyncReplacesOldPrice() {
        UUID skuId = skuWithPrice(3_990);
        productSync.sync(skuId);

        List<KnowledgeChunk> hits = store.search("演示商品 价格", 5);
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).content()).contains("39.90");

        // 改价后重新同步：旧价格必须消失
        jdbc.update("UPDATE sku SET price_amount = 4590 WHERE id = ?", skuId);
        productSync.sync(skuId);

        List<KnowledgeChunk> after = store.search("演示商品 价格", 5);
        assertThat(after).anySatisfy(chunk -> assertThat(chunk.content()).contains("45.90"));
        assertThat(after).noneSatisfy(chunk -> assertThat(chunk.content()).contains("39.90"));
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM knowledge_chunk WHERE source_id = ?", Integer.class,
                skuId.toString())).isEqualTo(1);
    }

    @Test
    void vectorRerankPrefersCloserChunk() {
        String a = "退款政策：签收 7 天内可申请，生鲜除外";
        String b = "发票说明：下单后可申请电子发票";
        store.replaceSourceWithEmbeddings("POLICY", "p1",
                List.of(new KnowledgeChunk(UUID.randomUUID(), "POLICY", "p1", "退款", a, "v1")),
                List.of(new float[]{1f, 0f}));
        store.replaceSourceWithEmbeddings("POLICY", "p2",
                List.of(new KnowledgeChunk(UUID.randomUUID(), "POLICY", "p2", "发票", b, "v1")),
                List.of(new float[]{0f, 1f}));

        // 两个块都能被关键词命中，靠向量决定顺序
        var hits = store.search("退款 发票", new float[]{1f, 0f}, 2);

        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).title()).isEqualTo("退款");
    }
}
