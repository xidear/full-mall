package com.fullmall.platform.export;

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

/** 异步导出任务：领取/完成/失败/归属/过期。真实 PostgreSQL（含 SKIP LOCKED）。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.export.max-rows=1"
        })
@ActiveProfiles("test")
class ExportJobServiceTest {

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
    ExportJobService service;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ExportWorker worker;

    @Autowired
    ExportStorage storage;

    @org.junit.jupiter.api.BeforeEach
    void clean() {
        jdbc.update("DELETE FROM export_job");
    }

    @Test
    void claimMarksRunningAndSecondClaimGetsNothing() {
        service.create("admin", "member", "FINANCE", Map.of("limit", 10), "req-1");

        var claimed = service.claimNext();
        assertThat(claimed).isNotNull();
        assertThat(claimed.status()).isEqualTo("RUNNING");

        assertThat(service.claimNext()).isNull();
    }

    @Test
    void succeedStoresRowCountAndPath() {
        UUID id = service.create("admin", "member", "OPS", Map.of(), null);
        service.claimNext();

        service.succeed(id, 42, "/tmp/export.csv");

        var job = service.find(id);
        assertThat(job.status()).isEqualTo("SUCCEEDED");
        assertThat(job.rowCount()).isEqualTo(42);
        assertThat(job.filePath()).isEqualTo("/tmp/export.csv");
    }

    @Test
    void workerWritesCsvAndMarksSucceeded() throws Exception {
        jdbc.update("DELETE FROM member");
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, 'M9001', '王小明', '13800000001', 'GOLD', 100, 0, 'CNY')
                """, UUID.randomUUID());
        UUID id = service.create("admin", "member", "OPS", Map.of(), null);

        assertThat(worker.runOnce()).isTrue();

        var job = service.find(id);
        assertThat(job.status()).isEqualTo("SUCCEEDED");
        assertThat(job.rowCount()).isEqualTo(1);
        String csv = new String(storage.read(job.filePath()), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("phoneMasked").contains("138****0001").doesNotContain("13800000001");
    }

    @Test
    void exportOverLimitFailsInsteadOfSilentlyTruncating() {
        // 上限设为 1，插入 2 条会员
        jdbc.update("DELETE FROM member");
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, 'M9101', '甲', '13800001001', 'GOLD', 0, 0, 'CNY'),
                       (?, 'M9102', '乙', '13800001002', 'GOLD', 0, 0, 'CNY')
                """, UUID.randomUUID(), UUID.randomUUID());
        UUID id = service.create("admin", "member", "OPS", Map.of(), null);

        assertThat(worker.runOnce()).isTrue();

        var job = service.find(id);
        assertThat(job.status()).isEqualTo("FAILED");
        assertThat(job.error()).contains("超过上限");
    }

    @Test
    void workerFailsJobForUnknownResource() {
        UUID id = service.create("admin", "unknown_resource", "OPS", Map.of(), null);

        assertThat(worker.runOnce()).isTrue();

        var job = service.find(id);
        assertThat(job.status()).isEqualTo("FAILED");
        assertThat(job.error()).contains("不支持的导出资源");
    }

    @Test
    void failRequiresReason() {
        UUID id = service.create("admin", "member", "OPS", Map.of(), null);

        assertThatThrownBy(() -> service.fail(id, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("原因");
    }

    @Test
    void otherUsersCannotReadJob() {
        UUID id = service.create("alice", "member", "OPS", Map.of(), null);

        assertThatThrownBy(() -> service.findForRequester(id, "bob"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void expiredFileIsGone() {
        UUID id = service.create("carol", "member", "OPS", Map.of(), null);
        jdbc.update("UPDATE export_job SET expires_at = now() - interval '1 day' WHERE id = ?", id);

        assertThatThrownBy(() -> service.findForRequester(id, "carol"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("过期");
    }

    @Test
    void purgesOnlyFinishedOldJobs() {
        jdbc.update("DELETE FROM export_job");
        jdbc.update("""
                INSERT INTO export_job (id, requester, resource, projection, status, created_at)
                VALUES (?, 'ops', 'member', 'OPS', 'SUCCEEDED', now() - interval '30 days')
                """, UUID.randomUUID());
        UUID runningId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO export_job (id, requester, resource, projection, status, created_at)
                VALUES (?, 'ops', 'member', 'OPS', 'RUNNING', now() - interval '30 days')
                """, runningId);
        UUID freshId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO export_job (id, requester, resource, projection, status)
                VALUES (?, 'ops', 'member', 'OPS', 'SUCCEEDED')
                """, freshId);

        int removed = service.purgeFinishedBefore(7);

        assertThat(removed).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM export_job WHERE id = ?", Integer.class, runningId)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM export_job WHERE id = ?", Integer.class, freshId)).isEqualTo(1);
    }

    @Test
    void purgeRejectsNonPositiveDays() {
        assertThatThrownBy(() -> service.purgeFinishedBefore(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("保留天数");
    }
}
