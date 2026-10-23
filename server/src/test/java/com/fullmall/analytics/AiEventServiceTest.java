package com.fullmall.analytics;

import com.fullmall.analytics.app.AiEventService;
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

/** 事件采集：类型校验、必填维度、热搜词与加购 Top。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class AiEventServiceTest {

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
    AiEventService events;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void recordsAndAggregates() {
        UUID skuId = UUID.randomUUID();
        events.record("SEARCH", null, "anon-1", null, " 有机牛奶 ", Map.of("from", "home"));
        events.record("SEARCH", null, "anon-2", null, "有机牛奶", null);
        events.record("SEARCH", null, "anon-3", null, "杯子", null);
        events.record("ADD_TO_CART", null, "anon-1", skuId, null, Map.of("qty", 2));

        var keywords = events.topKeywords(2);
        assertThat(keywords.get(0)).containsEntry("keyword", "有机牛奶");
        assertThat(((Number) keywords.get(0).get("hits")).longValue()).isEqualTo(2);

        var skus = events.topAddedSkus(1);
        assertThat(skus).hasSize(1);
        assertThat(skus.get(0)).containsEntry("skuId", skuId);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_event", Integer.class)).isEqualTo(4);
    }

    @Test
    void rejectsUnknownTypeAndMissingDimensions() {
        assertThatThrownBy(() -> events.record("CLICKED", null, "a", UUID.randomUUID(), null, null))
                .hasMessageContaining("事件类型只能是");
        assertThatThrownBy(() -> events.record("ADD_TO_CART", null, "a", null, null, null))
                .hasMessageContaining("必须带 skuId 或 keyword");
    }
}
