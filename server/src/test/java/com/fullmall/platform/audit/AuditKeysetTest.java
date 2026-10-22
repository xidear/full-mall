package com.fullmall.platform.audit;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 审计 keyset：两页不重不漏。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class AuditKeysetTest {

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
    AuditEventService auditEvents;

    @Autowired
    JdbcTemplate jdbc;

    private void event(int index) {
        jdbc.update("""
                INSERT INTO audit_event (id, event_type, actor, resource, resource_id, detail,
                                         request_id, created_at)
                VALUES (?, 'AK_TEST', 'tester', 'order', ?, 'd', 'r',
                        now() - (? || ' minutes')::interval)
                """, UUID.randomUUID(), "res-" + index, String.valueOf(index));
    }

    @Test
    void twoPagesDoNotOverlap() {
        for (int i = 1; i <= 5; i++) {
            event(i);
        }

        Map<String, Object> first = auditEvents.searchKeyset("AK_TEST", null, null, null, null, null, 2);
        var firstItems = (List<Map<String, Object>>) first.get("items");
        assertThat(firstItems).hasSize(2);

        Map<String, Object> second = auditEvents.searchKeyset("AK_TEST", null, null, null,
                java.time.Instant.parse((String) first.get("nextCreatedAt")),
                UUID.fromString((String) first.get("nextId")), 10);
        var secondItems = (List<Map<String, Object>>) second.get("items");

        assertThat(firstItems.stream().map(i -> i.get("id")).toList())
                .doesNotContainAnyElementsOf(secondItems.stream().map(i -> i.get("id")).toList());
        assertThat(firstItems.size() + secondItems.size()).isEqualTo(5);
    }
}
