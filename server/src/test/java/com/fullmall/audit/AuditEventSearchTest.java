package com.fullmall.audit;

import com.fullmall.platform.audit.AuditEventService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 审计查询：按类型/操作人过滤、分页、时间范围。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class AuditEventSearchTest {

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
    AuditEventService events;

    private void seed(String type, String actor) {
        events.record(new AuditEventService.AuditEvent(
                type, actor, "order", "o-1", "detail", null));
    }

    @Test
    void filtersByTypeAndActor() {
        seed("WITHDRAWAL_APPROVED", "alice");
        seed("PAYMENT_CONFIG_UPDATED", "alice");
        seed("WITHDRAWAL_APPROVED", "bob");

        var byType = events.search("WITHDRAWAL_APPROVED", null, null, null, 0, 50);
        assertThat((Long) byType.get("total")).isGreaterThanOrEqualTo(2);

        var byActor = events.search(null, "ali", null, null, 0, 50);
        assertThat((Long) byActor.get("total")).isGreaterThanOrEqualTo(2);
        @SuppressWarnings("unchecked")
        var items = (java.util.List<Map<String, Object>>) byActor.get("items");
        assertThat(items).allSatisfy(row -> assertThat(row.get("actor")).isEqualTo("alice"));
    }

    @Test
    void respectsTimeRangeAndPaging() {
        seed("PAGED_EVENT", "carol");

        var future = Instant.now().plus(1, ChronoUnit.DAYS);
        var noneYet = events.search("PAGED_EVENT", null, future, null, 0, 10);
        assertThat((Long) noneYet.get("total")).isZero();

        var paged = events.search("PAGED_EVENT", null, null, null, 0, 1);
        assertThat((Long) paged.get("total")).isGreaterThanOrEqualTo(1);
        assertThat(paged.get("size")).isEqualTo(1);
    }
}
