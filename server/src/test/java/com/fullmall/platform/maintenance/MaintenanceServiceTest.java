package com.fullmall.platform.maintenance;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class MaintenanceServiceTest {

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
    MaintenanceService maintenance;

    @Autowired
    JdbcTemplate jdbc;

    private UUID freshNotification;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM notification WHERE recipient_id LIKE 'maint-%'");
        jdbc.update("DELETE FROM export_job WHERE requester = 'maint'");
        jdbc.update("""
                INSERT INTO notification (id, recipient_type, recipient_id, event_type, channel,
                                          title, content, status, created_at)
                VALUES (?, 'MEMBER', 'maint-old', 'ORDER_PAID', 'IN_APP', '旧', 'x', 'SENT',
                        now() - interval '400 days')
                """, UUID.randomUUID());
        freshNotification = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO notification (id, recipient_type, recipient_id, event_type, channel,
                                          title, content, status)
                VALUES (?, 'MEMBER', 'maint-new', 'ORDER_PAID', 'IN_APP', '新', 'x', 'SENT')
                """, freshNotification);
        jdbc.update("""
                INSERT INTO export_job (id, requester, resource, projection, status, created_at)
                VALUES (?, 'maint', 'member', 'OPS', 'SUCCEEDED', now() - interval '30 days')
                """, UUID.randomUUID());
    }

    @Test
    void retentionRemovesOldDataAndKeepsFreshData() {
        var report = maintenance.runRetentionWithDefaults();

        assertThat(report.notifications()).isGreaterThanOrEqualTo(1);
        assertThat(report.exportJobs()).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE id = ?", Integer.class, freshNotification))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE recipient_id = 'maint-old'", Integer.class))
                .isZero();
    }
}
