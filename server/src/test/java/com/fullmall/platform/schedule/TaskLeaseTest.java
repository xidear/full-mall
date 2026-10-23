package com.fullmall.platform.schedule;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** 租约：同一任务只有一个持有者；过期后可被抢占；释放后可重取。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class TaskLeaseTest {

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
    TaskLease lease;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void onlyOneHolderAtATime() {
        assertThat(lease.tryAcquire("job-a", Duration.ofMinutes(5))).isTrue();
        // 第二个实例（同表不同 holder）抢不到
        int stolen = jdbc.update("""
                INSERT INTO scheduled_task_lease (code, holder, acquired_at, expires_at)
                VALUES ('job-a', 'other', now(), now() + interval '5 minutes')
                ON CONFLICT (code) DO UPDATE SET holder = 'other'
                 WHERE scheduled_task_lease.expires_at < now()
                """);
        assertThat(stolen).isZero();

        lease.release("job-a");
        assertThat(lease.tryAcquire("job-a", Duration.ofMinutes(5))).isTrue();
    }

    @Test
    void expiredLeaseCanBeTakenOver() {
        assertThat(lease.tryAcquire("job-b", Duration.ofSeconds(1))).isTrue();
        jdbc.update("UPDATE scheduled_task_lease SET expires_at = now() - interval '1 minute' WHERE code = 'job-b'");
        assertThat(lease.tryAcquire("job-b", Duration.ofMinutes(5))).isTrue();
    }
}
