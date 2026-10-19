package com.fullmall.platform.risk;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 风险预警（SEC-004 / SUP-034）。
 *
 * <p>职责边界：本类**只负责判定与落库**，事件来源是已有的审计（`audit_event`）与业务拒绝分支，
 * 不另建一套事实表。同一规则 + 同一对象在去重窗口内只留一条告警，避免刷屏。
 */
@Service
public class RiskAlertService {

    public enum Severity {
        LOW, MEDIUM, HIGH
    }

    private final JdbcTemplate jdbcTemplate;

    public RiskAlertService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record RiskAlert(UUID id, String ruleCode, Severity severity, String subject,
                            String detail, Instant createdAt) {
    }

    /**
     * 记录告警；去重窗口内已存在同规则同对象则跳过，返回 false。
     */
    public boolean raise(String ruleCode, Severity severity, String subject, String detail,
                         Duration dedupeWindow) {
        if (ruleCode == null || ruleCode.isBlank()) {
            throw new IllegalArgumentException("告警规则编码不能为空");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("告警对象不能为空，规则=" + ruleCode);
        }
        if (dedupeWindow == null || dedupeWindow.isNegative()) {
            throw new IllegalArgumentException("去重窗口非法: " + dedupeWindow);
        }

        Instant since = Instant.now().minus(dedupeWindow);
        Long existing = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM risk_alert
                 WHERE rule_code = ? AND subject = ? AND created_at >= ?
                """, Long.class, ruleCode, subject, java.sql.Timestamp.from(since));
        if (existing == null) {
            throw new IllegalStateException("告警去重查询返回 null: rule=" + ruleCode);
        }
        if (existing > 0) {
            return false;
        }

        int inserted = jdbcTemplate.update("""
                INSERT INTO risk_alert (id, rule_code, severity, subject, detail)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID(), ruleCode, severity.name(), subject, truncate(detail));
        if (inserted != 1) {
            throw new IllegalStateException("告警写入失败: " + ruleCode + "，影响行数=" + inserted);
        }
        return true;
    }

    public List<RiskAlert> findRecent(int limit) {
        if (limit <= 0 || limit > 500) {
            throw new IllegalArgumentException("查询条数必须在 1..500，实际=" + limit);
        }
        return jdbcTemplate.query("""
                SELECT id, rule_code, severity, subject, detail, created_at
                  FROM risk_alert ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> new RiskAlert(
                rs.getObject("id", UUID.class),
                rs.getString("rule_code"),
                Severity.valueOf(rs.getString("severity")),
                rs.getString("subject"),
                rs.getString("detail"),
                rs.getTimestamp("created_at").toInstant()), limit);
    }

    private String truncate(String detail) {
        if (detail == null || detail.length() <= 1024) {
            return detail;
        }
        return detail.substring(0, 1021) + "...";
    }
}