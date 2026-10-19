package com.fullmall.article;

import com.fullmall.article.app.ArticleAdminService;
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

/**
 * 文章后台流程（真实 PostgreSQL）：发布必须有英文默认译文，且只发布已审校译文。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ArticleAdminFlowTest {

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
    ArticleAdminService service;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void publishRequiresReviewedEnglishTranslation() {
        UUID id = service.create("help-shipping-" + UUID.randomUUID().toString().substring(0, 6),
                "HELP", "admin");

        // 只有中文译文、且未审校 → 不能发布
        service.upsertTranslation(id, "zh-CN", "配送说明", null, "正文", "admin");
        assertThatThrownBy(() -> service.publish(id, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("英文默认");

        // 补英文并审校 → 可以发布
        service.upsertTranslation(id, "en", "Shipping", null, "Content", "admin");
        service.review(id, "en", "admin");
        service.publish(id, "admin");

        assertThat(jdbc.queryForObject("SELECT status FROM article WHERE id = ?", String.class, id))
                .isEqualTo("PUBLISHED");
        assertThat(jdbc.queryForObject(
                "SELECT status FROM article_translation WHERE article_id = ? AND locale = 'en'",
                String.class, id)).isEqualTo("PUBLISHED");
        // 未审校的中文不会被发布
        assertThat(jdbc.queryForObject(
                "SELECT status FROM article_translation WHERE article_id = ? AND locale = 'zh-CN'",
                String.class, id)).isEqualTo("DRAFT");
    }

    @Test
    void reviewRejectsUnknownTranslation() {
        UUID id = service.create("page-about-" + UUID.randomUUID().toString().substring(0, 6), "PAGE", "admin");

        assertThatThrownBy(() -> service.review(id, "es", "admin"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不可送审");
    }
}
