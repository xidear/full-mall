package com.fullmall.platform.schedule;

import com.fullmall.platform.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 定时任务注册表：cron 与启停状态存库，运行期可改。
 *
 * <p>为什么用 `ThreadPoolTaskScheduler` 手动注册而不是 `@Scheduled`：
 * `@Scheduled` 的 cron 在启动时固定，改 cron 必须重启；这里改成运行期 `schedule(cron)`，
 * 后台改完立即生效。
 *
 * <p>注意：任务必须**幂等**（例如清理类任务），因为改 cron 后可能立刻触发一次。
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "app.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledTaskService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);

    private final JdbcTemplate jdbcTemplate;
    private final TaskLease lease;
    private final Map<String, ScheduledJob> jobs = new HashMap<>();
    private final ThreadPoolTaskScheduler scheduler;
    private final Map<String, ScheduledFuture<?>> running = new ConcurrentHashMap<>();

    public ScheduledTaskService(JdbcTemplate jdbcTemplate, TaskLease lease,
                                List<ScheduledJob> jobBeans) {
        this.jdbcTemplate = jdbcTemplate;
        this.lease = lease;
        for (ScheduledJob job : jobBeans) {
            if (jobs.put(job.code(), job) != null) {
                throw new IllegalStateException("定时任务编码重复：" + job.code());
            }
        }
        this.scheduler = new ThreadPoolTaskScheduler();
        this.scheduler.setPoolSize(Math.max(2, jobBeans.size()));
        this.scheduler.setThreadNamePrefix("scheduled-task-");
        this.scheduler.initialize();
    }

    public record TaskView(String code, String name, String cron, boolean enabled,
                           Instant lastRunAt, String lastStatus, String lastError,
                           Long lastDurationMs, Instant nextRunAt) {
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        for (ScheduledJob job : jobs.values()) {
            jdbcTemplate.update("""
                    INSERT INTO scheduled_task (code, name, cron, enabled)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (code) DO NOTHING
                    """, job.code(), job.name(), job.defaultCron(), job.enabledByDefault());
            scheduleIfEnabled(job.code());
        }
    }

    public List<TaskView> list() {
        return jdbcTemplate.query("""
                SELECT code, name, cron, enabled, last_run_at, last_status, last_error,
                       last_duration_ms FROM scheduled_task ORDER BY code
                """, (rs, rowNum) -> {
            String cron = rs.getString("cron");
            return new TaskView(
                    rs.getString("code"), rs.getString("name"), cron, rs.getBoolean("enabled"),
                    rs.getTimestamp("last_run_at") == null ? null : rs.getTimestamp("last_run_at").toInstant(),
                    rs.getString("last_status"), rs.getString("last_error"),
                    rs.getObject("last_duration_ms", Long.class),
                    nextRunAt(cron));
        });
    }

    /** 修改 cron / 启停；立即生效。 */
    public TaskView update(String code, String cron, boolean enabled) {
        requireJob(code);
        parseCron(cron);
        jdbcTemplate.update("""
                UPDATE scheduled_task SET cron = ?, enabled = ?, updated_at = now() WHERE code = ?
                """, cron.trim(), enabled, code);
        cancel(code);
        if (enabled) {
            schedule(code, cron.trim());
        }
        return find(code);
    }

    /** 手动触发一次（不管是否启用），用于验证与临时执行。 */
    public TaskView runNow(String code) {
        ScheduledJob job = requireJob(code);
        execute(job);
        return find(code);
    }

    public TaskView find(String code) {
        return list().stream().filter(task -> task.code().equals(code)).findFirst()
                .orElseThrow(() -> ApiException.notFound("SCHEDULED_TASK_NOT_FOUND", "任务不存在：" + code));
    }

    private ScheduledJob requireJob(String code) {
        ScheduledJob job = jobs.get(code);
        if (job == null) {
            throw ApiException.notFound("SCHEDULED_TASK_NOT_FOUND", "未注册的任务：" + code);
        }
        return job;
    }

    private void scheduleIfEnabled(String code) {
        Boolean enabled = jdbcTemplate.queryForObject(
                "SELECT enabled FROM scheduled_task WHERE code = ?", Boolean.class, code);
        if (Boolean.TRUE.equals(enabled)) {
            String cron = jdbcTemplate.queryForObject(
                    "SELECT cron FROM scheduled_task WHERE code = ?", String.class, code);
            parseCron(cron);
            schedule(code, cron);
        }
    }

    private void schedule(String code, String cron) {
        ScheduledJob job = requireJob(code);
        // CronExpression 本身不是 Trigger，用 CronTrigger 包装
        var future = scheduler.schedule(() -> execute(job),
                new org.springframework.scheduling.support.CronTrigger(cron, java.time.ZoneId.systemDefault()));
        running.put(code, future);
    }

    private void cancel(String code) {
        ScheduledFuture<?> future = running.remove(code);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void execute(ScheduledJob job) {
        long startedAt = System.currentTimeMillis();
        // 多实例互斥：抢不到租约说明别的实例在跑，记 SKIPPED（既不算成功也不报错）
        if (!lease.tryAcquire(job.code(), java.time.Duration.ofMinutes(30))) {
            log.info("定时任务跳过（其他实例持有租约） code={} holder={}", job.code(), lease.holder());
            jdbcTemplate.update(
                    "UPDATE scheduled_task SET last_status = 'SKIPPED', last_error = NULL WHERE code = ?",
                    job.code());
            return;
        }
        try {
            job.run();
            jdbcTemplate.update("""
                    UPDATE scheduled_task
                       SET last_run_at = now(), last_status = 'SUCCESS', last_error = NULL,
                           last_duration_ms = ?
                     WHERE code = ?
                    """, System.currentTimeMillis() - startedAt, job.code());
        } catch (Exception error) {
            log.error("定时任务执行失败 code={}", job.code(), error);
            String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            jdbcTemplate.update("""
                    UPDATE scheduled_task
                       SET last_run_at = now(), last_status = 'FAILED', last_error = ?,
                           last_duration_ms = ?
                     WHERE code = ?
                    """, message.length() > 500 ? message.substring(0, 500) : message,
                    System.currentTimeMillis() - startedAt, job.code());
        } finally {
            lease.release(job.code());
        }
    }

    private static CronExpression parseCron(String cron) {
        if (cron == null || cron.isBlank()) {
            throw ApiException.badRequest("INVALID_CRON", "cron 不能为空");
        }
        try {
            return CronExpression.parse(cron.trim());
        } catch (IllegalArgumentException error) {
            throw ApiException.badRequest("INVALID_CRON", "cron 非法：" + cron);
        }
    }

    private static Instant nextRunAt(String cron) {
        try {
            // 含星期字段的 cron 需要带时区的基准时间，Instant 会抛 UnsupportedTemporalTypeException
            var next = CronExpression.parse(cron).next(java.time.ZonedDateTime.now());
            return next == null ? null : next.toInstant();
        } catch (IllegalArgumentException error) {
            return null;
        }
    }
}
