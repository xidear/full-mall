package com.fullmall.platform.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 操作日志（SEC-003）：谁、何时、对哪个对象做了什么。
 *
 * <p>与 JaVers 的分工：JaVers 记录**实体字段级**变更（旧值/新值）；
 * 本类记录**行为级**事件（登录、开关、退款、库存调整），两者互补。
 *
 * <p>只提供写入与查询，不提供更新/删除（SEC-005 不可篡改）。
 */
@Service
public class AuditEventService {

    private final JdbcTemplate jdbcTemplate;

    public AuditEventService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record AuditEvent(String eventType, String actor, String resource,
                             String resourceId, String detail, String requestId) {
    }

    public void record(AuditEvent event) {
        if (event.eventType() == null || event.eventType().isBlank()) {
            throw new IllegalArgumentException("审计事件类型不能为空");
        }
        if (event.actor() == null || event.actor().isBlank()) {
            throw new IllegalArgumentException("审计事件必须记录操作人，事件=" + event.eventType());
        }
        int inserted = jdbcTemplate.update("""
                INSERT INTO audit_event (id, event_type, actor, resource, resource_id, detail, request_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), event.eventType(), event.actor(), event.resource(),
                event.resourceId(), truncate(event.detail()), event.requestId());
        if (inserted != 1) {
            throw new IllegalStateException("审计事件写入失败: " + event.eventType() + "，影响行数=" + inserted);
        }
    }

    public List<AuditEvent> findByType(String eventType) {
        return jdbcTemplate.query("""
                SELECT event_type, actor, resource, resource_id, detail, request_id
                  FROM audit_event WHERE event_type = ? ORDER BY created_at DESC
                """, (rs, rowNum) -> new AuditEvent(
                rs.getString("event_type"), rs.getString("actor"), rs.getString("resource"),
                rs.getString("resource_id"), rs.getString("detail"), rs.getString("request_id")),
                eventType);
    }

    /**
     * 查询审计事件（后台页面用）。
     *
     * <p>返回统一契约 `{items,page,size,total}`；时间范围按 UTC 日期闭区间前端传入。
     */
    public java.util.Map<String, Object> search(String eventType, String actor,
                                               java.time.Instant from, java.time.Instant to,
                                               int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page, 0) * safeSize;
        String typeFilter = eventType == null || eventType.isBlank() ? null : eventType.trim();
        String actorFilter = actor == null || actor.isBlank() ? null : actor.trim();

        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM audit_event
                 WHERE (?::text IS NULL OR event_type = ?::text)
                   AND (?::text IS NULL OR actor ILIKE '%' || ?::text || '%')
                   AND (?::timestamptz IS NULL OR created_at >= ?::timestamptz)
                   AND (?::timestamptz IS NULL OR created_at < ?::timestamptz)
                """, Long.class, typeFilter, typeFilter, actorFilter, actorFilter,
                ts(from), ts(from), ts(to), ts(to));

        var items = jdbcTemplate.query("""
                SELECT event_type, actor, resource, resource_id, detail, request_id, created_at
                  FROM audit_event
                 WHERE (?::text IS NULL OR event_type = ?::text)
                   AND (?::text IS NULL OR actor ILIKE '%' || ?::text || '%')
                   AND (?::timestamptz IS NULL OR created_at >= ?::timestamptz)
                   AND (?::timestamptz IS NULL OR created_at < ?::timestamptz)
                 ORDER BY created_at DESC
                 LIMIT ? OFFSET ?
                """, (rs, rowNum) -> new java.util.LinkedHashMap<>(java.util.Map.of(
                "eventType", rs.getString("event_type"),
                "actor", rs.getString("actor"),
                "resource", rs.getString("resource"),
                "resourceId", rs.getString("resource_id") == null ? "" : rs.getString("resource_id"),
                "detail", rs.getString("detail") == null ? "" : rs.getString("detail"),
                "requestId", rs.getString("request_id") == null ? "" : rs.getString("request_id"),
                "createdAt", rs.getTimestamp("created_at").toInstant().toString())),
                typeFilter, typeFilter, actorFilter, actorFilter, ts(from), ts(from), ts(to), ts(to),
                safeSize, offset);

        return java.util.Map.of("items", items, "page", Math.max(page, 0),
                "size", safeSize, "total", total == null ? 0L : total);
    }

    private java.sql.Timestamp ts(java.time.Instant instant) {
        return instant == null ? null : java.sql.Timestamp.from(instant);
    }

    /** detail 列宽 1024：超长截断并保留尾部省略标记，而不是静默丢内容。 */
    private String truncate(String detail) {
        if (detail == null || detail.length() <= 1024) {
            return detail;
        }
        return detail.substring(0, 1021) + "...";
    }
}