package com.fullmall.analytics;

import com.fullmall.analytics.action.AiActionDraftService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** N3：双签才可执行、同一人不能签两次、白名单与载荷校验。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class AiActionDraftTest {

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
    AiActionDraftService drafts;

    @Autowired
    JdbcTemplate jdbc;

    private String sku() {
        UUID productId = UUID.randomUUID();
        String code = "DRAFT-" + productId.toString().substring(0, 6);
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '草稿商品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 1000, 'CNY')
                """, UUID.randomUUID(), productId, code);
        return code;
    }

    @Test
    void requiresTwoDistinctApproversBeforeExecuting() {
        String skuCode = sku();
        var draft = drafts.create("sku.price.set",
                Map.of("skuCode", skuCode, "priceAmount", 1990), "ai-agent");

        assertThat(draft.status()).isEqualTo("PENDING_APPROVAL");
        assertThatThrownBy(() -> drafts.execute(draft.id(), "admin-a"))
                .hasMessageContaining("只有双签通过");

        var first = drafts.approve(draft.id(), "admin-a");
        assertThat(first.approvedBy1()).isEqualTo("admin-a");
        assertThatThrownBy(() -> drafts.approve(draft.id(), "admin-a"))
                .hasMessageContaining("同一人不能签两次");

        var second = drafts.approve(draft.id(), "admin-b");
        assertThat(second.status()).isEqualTo("APPROVED");

        var executed = drafts.execute(draft.id(), "admin-b");
        assertThat(executed.status()).isEqualTo("EXECUTED");
        assertThat(jdbc.queryForObject("SELECT price_amount FROM sku WHERE sku_code = ?",
                Long.class, skuCode)).isEqualTo(1990L);
    }

    @Test
    void rejectsUnsupportedActionAndBadPayload() {
        assertThatThrownBy(() -> drafts.create("sku.delete", Map.of(), "ai"))
                .hasMessageContaining("不支持的动作");

        var draft = drafts.create("sku.price.set", Map.of("skuCode", "NOPE", "priceAmount", 0), "ai");
        drafts.approve(draft.id(), "a1");
        drafts.approve(draft.id(), "a2");
        assertThatThrownBy(() -> drafts.execute(draft.id(), "a2"))
                .hasMessageContaining("价格必须大于 0");
    }

    /** 新增动作：商品上下架，同样必须双签。 */
    @Test
    void canArchiveProductAfterDualApproval() {
        UUID productId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '待下架', 'ACTIVE')", productId);

        var draft = drafts.create("product.status.set",
                Map.of("productId", productId.toString(), "status", "ARCHIVED"), "ai");
        drafts.approve(draft.id(), "a1");
        drafts.approve(draft.id(), "a2");
        var executed = drafts.execute(draft.id(), "a2");

        assertThat(executed.status()).isEqualTo("EXECUTED");
        assertThat(jdbc.queryForObject("SELECT status FROM product WHERE id = ?", String.class, productId))
                .isEqualTo("ARCHIVED");
    }
}
