package com.fullmall.platform.audit;

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

/** 审计 outbox：提交后行标记 DONE；失败累计到 FAILED 且保留行。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class AuditOutboxTest {

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
    AuditOutbox outbox;

    @Autowired
    JdbcTemplate jdbc;

    record Sample(UUID id, String name) {
    }

    @Test
    void enqueueStoresTypedJsonPayload() {
        UUID id = UUID.randomUUID();

        UUID outboxId = outbox.enqueue("tester", new Sample(id, "示例"));

        assertThat(jdbc.queryForObject(
                "SELECT entity_type FROM audit_outbox WHERE id = ?", String.class, outboxId))
                .isEqualTo(Sample.class.getName());
        assertThat(jdbc.queryForObject(
                "SELECT payload->>'name' FROM audit_outbox WHERE id = ?", String.class, outboxId))
                .isEqualTo("示例");
        assertThat(jdbc.queryForObject(
                "SELECT status FROM audit_outbox WHERE id = ?", String.class, outboxId))
                .isEqualTo("PENDING");
    }

    @Test
    void failuresAccumulateAndFinallyMarkFailed() {
        UUID outboxId = outbox.enqueue("tester", new Sample(UUID.randomUUID(), "失败示例"));

        for (int i = 0; i < 5; i++) {
            outbox.markFailure(outboxId, "模拟失败 " + i);
        }

        var row = jdbc.queryForMap(
                "SELECT status, attempts, last_error FROM audit_outbox WHERE id = ?", outboxId);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("attempts")).isEqualTo(5);
        assertThat(String.valueOf(row.get("last_error"))).contains("模拟失败");
    }
}
