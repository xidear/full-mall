package com.fullmall.platform.schedule;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 定时任务租约（多实例互斥）。
 *
 * <p>为什么不用 `pg_advisory_xact_lock`：那需要一个事务包住整个任务，长任务会把事务拖得很长；
 * 租约表是"短事务抢锁 + 运行期持有 + 结束释放/到期自动失效"，更适合批处理。
 *
 * <p>失败语义：抢不到返回 false（不是错误），由调用方记录 SKIPPED。
 */
@Component
public class TaskLease {

    private final JdbcTemplate jdbc;
    private final String holder = "node-" + UUID.randomUUID();

    public TaskLease(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean tryAcquire(String code, Duration ttl) {
        Instant expiresAt = Instant.now().plus(ttl);
        int updated = jdbc.update("""
                INSERT INTO scheduled_task_lease (code, holder, acquired_at, expires_at)
                VALUES (?, ?, now(), ?)
                ON CONFLICT (code) DO UPDATE
                   SET holder = EXCLUDED.holder,
                       acquired_at = now(),
                       expires_at = EXCLUDED.expires_at
                 WHERE scheduled_task_lease.expires_at < now()
                """, code, holder, java.sql.Timestamp.from(expiresAt));
        return updated == 1;
    }

    public void release(String code) {
        jdbc.update("DELETE FROM scheduled_task_lease WHERE code = ? AND holder = ?", code, holder);
    }

    public String holder() {
        return holder;
    }
}
