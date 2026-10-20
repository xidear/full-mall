package com.fullmall.platform.audit;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 审计快照保留：只删过期 commit 及其快照，不动新的。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class AuditRetentionTest {

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
    AuditRetentionService retention;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void purgesOnlyOldCommitsAndLeavesFreshOnes() {
        // 构造：一条 400 天前的 commit（带快照），一条新的
        Long oldCommit = jdbc.queryForObject(
                "INSERT INTO jv_commit (author, commit_date) VALUES ('ops', now() - interval '400 days') RETURNING commit_pk",
                Long.class);
        Long newCommit = jdbc.queryForObject(
                "INSERT INTO jv_commit (author, commit_date) VALUES ('ops', now()) RETURNING commit_pk",
                Long.class);
        Long globalId = jdbc.queryForObject(
                "INSERT INTO jv_global_id (local_id, type_name) VALUES ('x', 'test.Entity') RETURNING global_id_pk",
                Long.class);
        jdbc.update("""
                INSERT INTO jv_snapshot (type, version, state, global_id_fk, commit_fk)
                VALUES ('INITIAL', 1, '{}', ?, ?)
                """, globalId, oldCommit);
        jdbc.update("""
                INSERT INTO jv_snapshot (type, version, state, global_id_fk, commit_fk)
                VALUES ('INITIAL', 1, '{}', ?, ?)
                """, globalId, newCommit);

        var purged = retention.purgeOlderThan(365);

        assertThat(purged.commits()).isEqualTo(1);
        assertThat(purged.snapshots()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM jv_commit WHERE commit_pk = ?", Integer.class, oldCommit)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM jv_commit WHERE commit_pk = ?", Integer.class, newCommit)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM jv_snapshot WHERE commit_fk = ?", Integer.class, newCommit)).isEqualTo(1);
    }

    @Test
    void rejectsNonPositiveDays() {
        assertThatThrownBy(() -> retention.purgeOlderThan(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("保留天数");
    }
}
