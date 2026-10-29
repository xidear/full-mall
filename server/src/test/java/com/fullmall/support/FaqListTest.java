package com.fullmall.support;

import com.fullmall.support.app.FaqService;
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

/**
 * FAQ 后台列表：关键词为空时必须走"无过滤"分支。
 *
 * <p>回归背景：原来是 `(? IS NULL OR ...)`，Postgres 无法推断参数类型
 * （could not determine data type of parameter $1），接口直接 500。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class FaqListTest {

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
    FaqService faqs;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void listsWithoutKeywordAndFiltersWithKeyword() {
        jdbc.update("INSERT INTO faq_entry (id, question, answer) VALUES (?, ?, ?)",
                UUID.randomUUID(), "配送要多久", "同城次日达");
        jdbc.update("INSERT INTO faq_entry (id, question, answer) VALUES (?, ?, ?)",
                UUID.randomUUID(), "如何退货", "在订单页申请");

        assertThat(faqs.list(null)).isNotEmpty();
        assertThat(faqs.list("  ")).isNotEmpty();

        var filtered = faqs.list("配送");
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).question()).isEqualTo("配送要多久");
    }
}
