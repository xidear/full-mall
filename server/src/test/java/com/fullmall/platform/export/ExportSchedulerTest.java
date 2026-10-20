package com.fullmall.platform.export;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 调度器端到端：开启 worker 后，PENDING 任务应被自动执行完成。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                                                "app.export.worker.interval-ms=200",
                "app.export.worker.initial-delay-ms=200",
                "app.export.storage-dir=/tmp/fullmall-exports-test"
        })
@ActiveProfiles({"test", "dev"})
class ExportSchedulerTest {

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
    ExportJobService jobs;

    @Autowired
    ExportJobScheduler scheduler;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void pendingJobIsProcessedByScheduler() throws Exception {
        jdbc.update("DELETE FROM export_job");
        jdbc.update("DELETE FROM member");
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, 'M9002', '李静', '13800000002', 'STANDARD', 10, 0, 'CNY')
                """, UUID.randomUUID());

        UUID id = jobs.create("scheduler", "member", "OPS", Map.of(), null);

        // 直接驱动调度方法（真实代码路径），不依赖 Spring 全局调度，避免跨测试干扰
        scheduler.run();

        Instant deadline = Instant.now().plus(Duration.ofSeconds(20));
        String status = "PENDING";
        while (Instant.now().isBefore(deadline)) {
            status = jobs.find(id).status();
            if ("SUCCEEDED".equals(status) || "FAILED".equals(status)) {
                break;
            }
            Thread.sleep(200);
        }

        assertThat(status).isEqualTo("SUCCEEDED");
        assertThat(jobs.find(id).filePath()).isNotBlank();
    }
}
