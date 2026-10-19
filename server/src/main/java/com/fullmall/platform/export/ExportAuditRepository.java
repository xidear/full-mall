package com.fullmall.platform.export;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 导出任务审计仓储：只追加，不提供更新/删除方法（SEC-005 不可篡改）。
 */
@Repository
public class ExportAuditRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExportAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record ExportAudit(UUID id, String resource, String scopeKind, String actor,
                              int rowCount, String columns, String requestId) {
    }

    public void record(ExportAudit audit) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO export_task (id, resource, scope_kind, actor, row_count, columns, request_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, audit.id(), audit.resource(), audit.scopeKind(), audit.actor(),
                audit.rowCount(), audit.columns(), audit.requestId());
        if (inserted != 1) {
            throw new IllegalStateException(
                    "导出审计写入失败: resource=" + audit.resource() + "，影响行数=" + inserted);
        }
    }

    public List<ExportAudit> findByActor(String actor) {
        return jdbcTemplate.query("""
                SELECT id, resource, scope_kind, actor, row_count, columns, request_id
                  FROM export_task WHERE actor = ? ORDER BY created_at DESC
                """, (rs, rowNum) -> new ExportAudit(
                rs.getObject("id", UUID.class),
                rs.getString("resource"),
                rs.getString("scope_kind"),
                rs.getString("actor"),
                rs.getInt("row_count"),
                rs.getString("columns"),
                rs.getString("request_id")), actor);
    }
}