package com.fullmall.catalog;

import com.fullmall.catalog.app.ProductTranslationWorkflowService;
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

/** H9：译文可录入（DRAFT），同一商品同语言覆盖；换 zh-CN 或不给标题拒绝。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class TranslationDraftTest {

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
    ProductTranslationWorkflowService workflow;

    @Autowired
    JdbcTemplate jdbc;

    private UUID product() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '有机鲜牛奶', 'ACTIVE')", id);
        return id;
    }

    @Test
    void savesDraftAndOverwritesSameLocale() {
        UUID productId = product();

        var draft = workflow.saveDraft(productId, "en", "Organic Milk", "Fresh daily", "admin");
        assertThat(draft.title()).isEqualTo("Organic Milk");
        assertThat(draft.status().name()).isEqualTo("DRAFT");

        var replaced = workflow.saveDraft(productId, "en", "Organic Whole Milk", null, "admin");
        assertThat(replaced.title()).isEqualTo("Organic Whole Milk");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM product_translation WHERE product_id = ? AND locale = 'en'",
                Integer.class, productId)).isEqualTo(1);
    }

    @Test
    void rejectsBlankTitleAndSourceLocale() {
        UUID productId = product();
        assertThatThrownBy(() -> workflow.saveDraft(productId, "en", " ", null, "admin"))
                .hasMessageContaining("译文标题不能为空");
        assertThatThrownBy(() -> workflow.saveDraft(productId, "zh-CN", "中文", null, "admin"))
                .hasMessageContaining("非 zh-CN");
    }
}
