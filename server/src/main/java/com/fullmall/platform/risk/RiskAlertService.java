package com.fullmall.platform.risk;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 风险预警（SEC-004 / SUP-034）。
 *
 * <p>职责边界：本类**只负责判定与落库**，事件来源是已有的审计（audit_event）与业务拒绝分支，
 * 不另建一套事实表。同一规则 + 同一对象在去重窗口内只留一条告警，避免刷屏。
 *
 * <p>外部通知交给 {@link AlertDispatchService} 异步派发：告警是网络调用，
 * 放在业务请求线程上会把一次回调拖慢十几秒。
 */
@Service
public class RiskAlertService {

    /** 分级与 AGENTS §10.5 对齐：FATAL/HIGH 立即通知值班人，MEDIUM/LOW 只进管理端。 */
    public enum Severity {
        FATAL, HIGH, MEDIUM, LOW
    }

    private final JdbcTemplate jdbcTemplate;
    private final AlertDispatchService alerts;

    public RiskAlertService(JdbcTemplate jdbcTemplate, AlertDispatchService alerts) {
        this.jdbcTemplate = jdbcTemplate;
        this.alerts = alerts;
    }

    public record RiskAlert(UUID id, String ruleCode, Severity severity, String subject,
                            String detail, Instant createdAt, boolean alerted, Instant alertedAt,
                            String alertChannel, String alertError, Instant recoveredAt,
                            Instant acknowledgedAt, String acknowledgedBy, int escalationLevel,
                            Instant escalatedAt) {
    }

    /**
     * 记录告警；去重窗口内已存在同规则同对象则跳过，返回 false。
     *
     * <p>写入成功后异步派发外部通知——先落库再发：发送失败也要留下这条告警，
     * 否则「告警没发出去」会连记录一起消失。
     */
    @Transactional
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

        UUID id = UUID.randomUUID();
        int inserted = jdbcTemplate.update("""
                INSERT INTO risk_alert (id, rule_code, severity, subject, detail)
                VALUES (?, ?, ?, ?, ?)
                """, id, ruleCode, severity.name(), subject, truncate(detail));
        if (inserted != 1) {
            throw new IllegalStateException("告警写入失败: " + ruleCode + "，影响行数=" + inserted);
        }
        alerts.dispatchAsync(id);
        return true;
    }

    /** 标记恢复并发一条恢复通知（AGENTS §10.5：告警必须可恢复通知）。 */
    @Transactional
    public RiskAlert recover(UUID id) {
        List<Boolean> rows = jdbcTemplate.query("""
                SELECT recovered_at FROM risk_alert WHERE id = ?
                """, (rs, rowNum) -> rs.getTimestamp("recovered_at") != null, id);
        if (rows.isEmpty()) {
            throw ApiException.notFound("RISK_ALERT_NOT_FOUND", "告警不存在: " + id);
        }
        if (Boolean.TRUE.equals(rows.get(0))) {
            throw ApiException.conflict("RISK_ALERT_ALREADY_RECOVERED", "该告警已标记恢复: " + id);
        }
        int updated = jdbcTemplate.update("""
                UPDATE risk_alert SET recovered_at = now() WHERE id = ? AND recovered_at IS NULL
                """, id);
        if (updated != 1) {
            throw new IllegalStateException("标记恢复失败: " + id + "，影响行数=" + updated);
        }
        alerts.dispatchRecoveryAsync(id);
        return findById(id);
    }

    /**
     * 确认处理：值班人认领这条告警，升级巡检从此不再升级它。
     *
     * <p>为什么必须有：只有「恢复」没有「确认」时，值班人半夜被叫起来看一眼发现是已知问题，
     * 既不能收掉告警（问题还在）也不能让它停下来（会一直升级刷屏）。确认就是第三种状态。
     */
    @Transactional
    public RiskAlert acknowledge(UUID id, String operator) {
        if (operator == null || operator.isBlank()) {
            throw ApiException.badRequest("RISK_ALERT_OPERATOR_REQUIRED", "确认告警必须带操作人");
        }
        List<Object[]> rows = jdbcTemplate.query("""
                SELECT acknowledged_at, recovered_at FROM risk_alert WHERE id = ?
                """, (rs, rowNum) -> new Object[]{rs.getTimestamp("acknowledged_at"),
                rs.getTimestamp("recovered_at")}, id);
        if (rows.isEmpty()) {
            throw ApiException.notFound("RISK_ALERT_NOT_FOUND", "告警不存在: " + id);
        }
        if (rows.get(0)[0] != null) {
            throw ApiException.conflict("RISK_ALERT_ALREADY_ACKNOWLEDGED", "该告警已被确认: " + id);
        }
        if (rows.get(0)[1] != null) {
            throw ApiException.conflict("RISK_ALERT_ALREADY_RECOVERED", "该告警已恢复，无需确认: " + id);
        }
        int updated = jdbcTemplate.update("""
                UPDATE risk_alert SET acknowledged_at = now(), acknowledged_by = ?
                 WHERE id = ? AND acknowledged_at IS NULL
                """, operator.trim(), id);
        if (updated != 1) {
            throw new IllegalStateException("确认告警失败: " + id + "，影响行数=" + updated);
        }
        return findById(id);
    }

    /**
     * 待升级的告警：未恢复、未确认、级别未到上限，且距上次升级（没升级过则从创建时刻算）已超过该级别的间隔。
     *
     * <p>间隔按严重度分档（FATAL 更急）；放在这里而不是塞进 SQL，是为了让「为什么是这个间隔」有地方写清。
     */
    public List<RiskAlert> escalationCandidates(Instant now, java.time.Duration fatalInterval,
                                                java.time.Duration highInterval, int maxLevel) {
        if (now == null) {
            throw new IllegalArgumentException("升级巡检缺少当前时间");
        }
        if (maxLevel < 1) {
            throw new IllegalArgumentException("升级上限必须 ≥ 1，实际=" + maxLevel);
        }
        return jdbcTemplate.query("""
                SELECT id, rule_code, severity, subject, detail, created_at, alerted, alerted_at,
                       alert_channel, alert_error, recovered_at, acknowledged_at, acknowledged_by,
                       escalation_level, escalated_at
                  FROM risk_alert
                 WHERE recovered_at IS NULL AND acknowledged_at IS NULL
                   AND severity IN ('FATAL', 'HIGH') AND escalation_level < ?
                 ORDER BY created_at
                """, (rs, rowNum) -> map(rs), maxLevel).stream()
                .filter(alert -> {
                    java.time.Duration interval = alert.severity() == Severity.FATAL ? fatalInterval : highInterval;
                    Instant since = alert.escalatedAt() != null ? alert.escalatedAt() : alert.createdAt();
                    return !now.isBefore(since.plus(interval));
                })
                .toList();
    }

    /** 记一次升级（级别 +1）并异步派发升级通知；并发安全用「级别未变」条件更新兜底。 */
    @Transactional
    public RiskAlert escalate(UUID id) {
        List<Integer> levels = jdbcTemplate.query("""
                SELECT escalation_level FROM risk_alert
                 WHERE id = ? AND recovered_at IS NULL AND acknowledged_at IS NULL
                """, (rs, rowNum) -> rs.getInt("escalation_level"), id);
        if (levels.isEmpty()) {
            throw ApiException.conflict("RISK_ALERT_NOT_ESCALATABLE", "该告警已恢复或已确认，不再升级: " + id);
        }
        int current = levels.get(0);
        int updated = jdbcTemplate.update("""
                UPDATE risk_alert SET escalation_level = escalation_level + 1, escalated_at = now()
                 WHERE id = ? AND escalation_level = ? AND recovered_at IS NULL AND acknowledged_at IS NULL
                """, id, current);
        if (updated != 1) {
            throw ApiException.conflict("RISK_ALERT_ESCALATION_RACED", "告警升级被并发修改，请重试: " + id);
        }
        alerts.dispatchEscalationAsync(id, current + 1);
        return findById(id);
    }

    public RiskAlert findById(UUID id) {
        return jdbcTemplate.query("""
                SELECT id, rule_code, severity, subject, detail, created_at, alerted, alerted_at,
                       alert_channel, alert_error, recovered_at, acknowledged_at, acknowledged_by,
                       escalation_level, escalated_at
                  FROM risk_alert WHERE id = ?
                """, (rs, rowNum) -> map(rs), id)
                .stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("RISK_ALERT_NOT_FOUND", "告警不存在: " + id));
    }

    public List<RiskAlert> findRecent(int limit) {
        if (limit <= 0 || limit > 500) {
            throw new IllegalArgumentException("查询条数必须在 1..500，实际=" + limit);
        }
        return jdbcTemplate.query("""
                SELECT id, rule_code, severity, subject, detail, created_at, alerted, alerted_at,
                       alert_channel, alert_error, recovered_at, acknowledged_at, acknowledged_by,
                       escalation_level, escalated_at
                  FROM risk_alert ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> map(rs), limit);
    }

    private RiskAlert map(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.sql.Timestamp alertedAt = rs.getTimestamp("alerted_at");
        java.sql.Timestamp recoveredAt = rs.getTimestamp("recovered_at");
        java.sql.Timestamp acknowledgedAt = rs.getTimestamp("acknowledged_at");
        java.sql.Timestamp escalatedAt = rs.getTimestamp("escalated_at");
        return new RiskAlert(rs.getObject("id", UUID.class), rs.getString("rule_code"),
                Severity.valueOf(rs.getString("severity")), rs.getString("subject"),
                rs.getString("detail"), rs.getTimestamp("created_at").toInstant(),
                rs.getBoolean("alerted"), alertedAt == null ? null : alertedAt.toInstant(),
                rs.getString("alert_channel"), rs.getString("alert_error"),
                recoveredAt == null ? null : recoveredAt.toInstant(),
                acknowledgedAt == null ? null : acknowledgedAt.toInstant(),
                rs.getString("acknowledged_by"), rs.getInt("escalation_level"),
                escalatedAt == null ? null : escalatedAt.toInstant());
    }

    private String truncate(String detail) {
        if (detail == null || detail.length() <= 1024) {
            return detail;
        }
        return detail.substring(0, 1021) + "...";
    }
}
