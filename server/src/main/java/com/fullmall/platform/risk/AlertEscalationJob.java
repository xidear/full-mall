package com.fullmall.platform.risk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 告警升级巡检：FATAL/HIGH 长时间没人确认就按级别再推一轮（AGENTS §10.5）。
 *
 * <p>为什么按严重度分档而不是一刀切：FATAL 是「资金/全站」级别，15 分钟没动静必须再喊一次；
 * HIGH 给 1 小时，避免同一个已知问题每小时把值班人叫醒三回。上限 3 级后停止升级
 * （继续升级只会训练大家忽略告警，真正的收敛手段是人去确认或标记恢复）。
 *
 * <p>巡检只读「未恢复 + 未确认」的行（部分索引覆盖），并且每条告警的升级都在
 * {@link RiskAlertService#escalate} 里用「级别未变」条件更新兜底，多副本同时巡检不会重复升级。
 */
@Component
public class AlertEscalationJob {

    private static final Logger log = LoggerFactory.getLogger(AlertEscalationJob.class);

    /** FATAL 首次升级间隔；之后每级同样间隔。 */
    static final Duration FATAL_INTERVAL = Duration.ofMinutes(15);
    /** HIGH 首次升级间隔。 */
    static final Duration HIGH_INTERVAL = Duration.ofHours(1);
    /** 最多升级到第几级。 */
    static final int MAX_LEVEL = 3;

    private final RiskAlertService alerts;

    public AlertEscalationJob(RiskAlertService alerts) {
        this.alerts = alerts;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 45_000)
    public void run() {
        List<RiskAlertService.RiskAlert> due =
                alerts.escalationCandidates(Instant.now(), FATAL_INTERVAL, HIGH_INTERVAL, MAX_LEVEL);
        for (RiskAlertService.RiskAlert alert : due) {
            try {
                RiskAlertService.RiskAlert escalated = alerts.escalate(alert.id());
                log.warn("告警升级 alertId={} rule={} level={}", alert.id(), alert.ruleCode(),
                        escalated.escalationLevel());
            } catch (RuntimeException error) {
                // 一条失败不影响其它告警：可能是被并发确认了（预期内），也可能是真的写库失败
                log.warn("告警升级失败 alertId={} rule={}", alert.id(), alert.ruleCode(), error);
            }
        }
    }
}
