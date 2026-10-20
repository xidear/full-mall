package com.fullmall.platform.export;

import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 异步导出任务（A6）。
 *
 * <p>为什么必须异步：大导出会长时间占用请求线程与连接；这里只创建任务，由 worker 抢占执行。
 *
 * <p>并发安全：worker 用 `FOR UPDATE SKIP LOCKED` 领取任务，多副本不会重复执行同一任务。
 * 文件与状态分离：`file_path` 落对象存储/本地目录，任务表只记引用。
 */
@Service
public class ExportJobService {

    /** 导出文件保留期（DECISIONS：7 天）。 */
    public static final Duration RETENTION = Duration.ofDays(7);

    private final JdbcTemplate jdbcTemplate;
    private final AuditEventService auditEvents;

    public ExportJobService(JdbcTemplate jdbcTemplate, AuditEventService auditEvents) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditEvents = auditEvents;
    }

    public record ExportJob(UUID id, String requester, String resource, String projection,
                            String status, Integer rowCount, String filePath, String error,
                            Instant createdAt, Instant expiresAt) {
    }

    @Transactional
    public UUID create(String requester, String resource, String projection,
                       Map<String, Object> params, String requestId) {
        if (requester == null || requester.isBlank()) {
            throw ApiException.badRequest("REQUESTER_REQUIRED", "导出任务必须记录发起人");
        }
        if (resource == null || resource.isBlank()) {
            throw ApiException.badRequest("RESOURCE_REQUIRED", "必须指定导出资源");
        }
        if (projection == null || projection.isBlank()) {
            throw ApiException.badRequest("PROJECTION_REQUIRED", "必须指定导出投影（决定列集合）");
        }
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO export_job (id, requester, resource, projection, params, status, expires_at)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), 'PENDING', ?)
                """, id, requester.trim(), resource.trim(), projection.trim(),
                toJson(params), Timestamp.from(Instant.now().plus(RETENTION)));
        auditEvents.record(new AuditEventService.AuditEvent(
                "EXPORT_JOB_CREATED", requester.trim(), resource.trim(), id.toString(),
                "projection=" + projection.trim(), requestId));
        return id;
    }

    /**
     * 领取一个待执行任务；没有则返回 null。`SKIP LOCKED` 保证多副本不重复领取。
     */
    /**
     * 清理已结束（SUCCEEDED/FAILED）且早于保留期的导出任务。
     *
     * <p>只删**已结束**的任务：进行中的任务被删会导致 worker 找不到记录。
     *
     * @param days 保留天数，必须 &gt; 0
     * @return 删除条数
     */
    @Transactional
    public int purgeFinishedBefore(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("导出保留天数必须大于 0，实际=" + days);
        }
        return jdbcTemplate.update("""
                DELETE FROM export_job
                 WHERE status IN ('SUCCEEDED', 'FAILED')
                   AND created_at < now() - make_interval(days => ?)
                """, days);
    }

    @Transactional
    public ExportJob claimNext() {
        List<UUID> ids = jdbcTemplate.query("""
                SELECT id FROM export_job
                 WHERE status = 'PENDING'
                 ORDER BY created_at
                 FOR UPDATE SKIP LOCKED
                 LIMIT 1
                """, (rs, rowNum) -> rs.getObject("id", UUID.class));
        if (ids.isEmpty()) {
            return null;
        }
        UUID id = ids.get(0);
        jdbcTemplate.update("""
                UPDATE export_job SET status = 'RUNNING', started_at = now() WHERE id = ?
                """, id);
        return find(id);
    }

    @Transactional
    public void succeed(UUID id, int rowCount, String filePath) {
        int updated = jdbcTemplate.update("""
                UPDATE export_job
                   SET status = 'SUCCEEDED', row_count = ?, file_path = ?, finished_at = now()
                 WHERE id = ? AND status = 'RUNNING'
                """, rowCount, filePath, id);
        if (updated != 1) {
            throw new IllegalStateException("导出任务完成状态写入失败（可能未在 RUNNING 状态）: " + id);
        }
    }

    @Transactional
    public void fail(UUID id, String error) {
        if (error == null || error.isBlank()) {
            throw new IllegalArgumentException("失败必须记录原因: " + id);
        }
        jdbcTemplate.update("""
                UPDATE export_job SET status = 'FAILED', error = ?, finished_at = now()
                 WHERE id = ? AND status IN ('PENDING', 'RUNNING')
                """, truncate(error), id);
    }

    /** 只允许发起人查看自己的任务；过期文件返回 410。 */
    public ExportJob findForRequester(UUID id, String requester) {
        ExportJob job = find(id);
        if (requester == null || !job.requester().equals(requester.trim())) {
            throw ApiException.notFound("EXPORT_JOB_NOT_FOUND", "导出任务不存在: " + id);
        }
        if (job.expiresAt() != null && job.expiresAt().isBefore(Instant.now())) {
            throw new ApiException(org.springframework.http.HttpStatus.GONE, "EXPORT_JOB_EXPIRED",
                    "导出文件已过期，请重新导出");
        }
        return job;
    }

    public ExportJob find(UUID id) {
        return jdbcTemplate.query("""
                SELECT id, requester, resource, projection, status, row_count, file_path, error,
                       created_at, expires_at
                  FROM export_job WHERE id = ?
                """, (rs, rowNum) -> new ExportJob(
                rs.getObject("id", UUID.class),
                rs.getString("requester"),
                rs.getString("resource"),
                rs.getString("projection"),
                rs.getString("status"),
                (Integer) rs.getObject("row_count"),
                rs.getString("file_path"),
                rs.getString("error"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant()),
                id).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("EXPORT_JOB_NOT_FOUND", "导出任务不存在: " + id));
    }

    private String toJson(Map<String, Object> params) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(params == null ? Map.of() : params);
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException("导出参数序列化失败", error);
        }
    }

    private String truncate(String value) {
        return value.length() <= 1024 ? value : value.substring(0, 1021) + "...";
    }
}