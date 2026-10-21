package com.fullmall.audit;

import com.fullmall.platform.audit.AuditTrail;
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
 * H1 真实调用：造一条 PENDING 的 outbox 行，调用恢复，验证被消费为 DONE。
 *
 * <p>此前只有"任务存在/默认关闭"的断言，**恢复逻辑从没真跑过**。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class AuditRecoveryRealRunTest {

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
    AuditTrail auditTrail;

    @Autowired
    com.fullmall.platform.audit.AuditOutbox auditOutbox;

    @Autowired
    com.fullmall.asset.infra.AssetRepository assetRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void recoverConsumesPendingRow() {
        // 用系统自己写出的 payload：先入队（PENDING），再走恢复路径消费
        var entity = new com.fullmall.asset.domain.AssetEntity(
                "恢复测试", "http://example.com/a.png", "IMAGE", "tester");
        entity.setId(UUID.randomUUID());   // 该实体 id 需手工赋值（无 @GeneratedValue）
        entity.setCreatedAt(java.time.Instant.now());
        entity.setUpdatedAt(java.time.Instant.now());
        entity = assetRepository.saveAndFlush(entity);
        auditOutbox.enqueue("test:recovery", entity);
        UUID outboxId = jdbc.queryForObject(
                "SELECT id FROM audit_outbox WHERE status = 'PENDING' ORDER BY created_at DESC LIMIT 1",
                UUID.class);

        long before = auditTrail.pendingRows();
        assertThat(before).isGreaterThanOrEqualTo(1);

        auditTrail.recoverOrphans();

        var row = jdbc.queryForMap(
                "SELECT status, attempts, last_error FROM audit_outbox WHERE id = ?", outboxId);

        // 按 id 重载实体后，恢复应真正提交成功
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("last_error")).isNull();
        assertThat(auditTrail.pendingRows()).isLessThan(before);
    }
}
