package com.fullmall.i18n;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 文案：en 取英文、缺失回退中文、未知 key 返回 key 本身、白名单校验。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class UiTranslationServiceTest {

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
    UiTranslationService translations;

    @Test
    void returnsEnglishWithChineseFallback() {
        assertThat(translations.text("en", "nav.home")).isEqualTo("Home");

        translations.upsert("en", Map.of("custom.onlyZh", "").isEmpty()
                ? Map.of() : Map.of("custom.onlyEn", "Only English"));
        translations.upsert("zh-CN", Map.of("custom.onlyZh", "只有中文"));

        Map<String, String> en = translations.bundle("en");
        assertThat(en).containsEntry("custom.onlyZh", "只有中文");
        assertThat(translations.text("en", "no.such.key")).isEqualTo("no.such.key");
    }

    @Test
    void rejectsUnsupportedLocaleAndEmptyValues() {
        assertThatThrownBy(() -> translations.upsert("fr", Map.of("a", "b")))
                .hasMessageContaining("不支持的语言");
        assertThatThrownBy(() -> translations.upsert("en", Map.of("k", " ")))
                .hasMessageContaining("文案值不能为空");
    }
}
