package com.fullmall.platform.risk;

import com.fullmall.platform.channel.ChannelSendService;
import com.fullmall.platform.channel.IntegrationRouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 告警派发：把 risk_alert 里的 FATAL/HIGH 事件真正推给值班人（AGENTS §10.5）。
 *
 * <p>派发结果必须落库（alerted / alerted_at / alert_channel / alert_error）：
 * 「告警没发出去」和「没有告警」是完全不同的两件事，管理端必须能一眼区分。
 *
 * <p>发送顺序：先群机器人（不依赖收件人、最快），再按需补短信与邮件；
 * 任一通道失败都不影响其它通道，最后把失败原因汇总写回。
 */
@Service
public class AlertDispatchService {

    private static final Logger log = LoggerFactory.getLogger(AlertDispatchService.class);

    private final ChannelSendService channels;
    private final IntegrationRouteService routes;
    private final JdbcTemplate jdbc;

    public AlertDispatchService(ChannelSendService channels, IntegrationRouteService routes,
                                JdbcTemplate jdbc) {
        this.channels = channels;
        this.routes = routes;
        this.jdbc = jdbc;
    }

    /** 异步入口：raise() 落库后调用，不阻塞业务请求。 */
    @Async("alertExecutor")
    public void dispatchAsync(UUID alertId) {
        try {
            dispatchNow(alertId, false);
        } catch (RuntimeException error) {
            log.error("告警派发异常 alertId={}", alertId, error);
        }
    }

    @Async("alertExecutor")
    public void dispatchRecoveryAsync(UUID alertId) {
        try {
            dispatchNow(alertId, true);
        } catch (RuntimeException error) {
            log.error("恢复通知派发异常 alertId={}", alertId, error);
        }
    }

    /** 同步实现：测试直接调用，生产通过异步入口触发。 */
    public void dispatchNow(UUID alertId, boolean recovery) {
        List<String> sent = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        String ruleCode;
        String severity;
        String subject;
        String detail;
        try {
            var row = jdbc.query("""
                    SELECT rule_code, severity, subject, detail FROM risk_alert WHERE id = ?
                    """, (rs, rn) -> new Object[]{rs.getString(1), rs.getString(2),
                    rs.getString(3), rs.getString(4)}, alertId);
            if (row.isEmpty()) {
                log.warn("告警不存在，跳过派发：{}", alertId);
                return;
            }
            ruleCode = (String) row.get(0)[0];
            severity = (String) row.get(0)[1];
            subject = (String) row.get(0)[2];
            detail = (String) row.get(0)[3];
        } catch (RuntimeException error) {
            log.error("读取告警失败，无法派发：{}", alertId, error);
            return;
        }

        if (!recovery && !"FATAL".equals(severity) && !"HIGH".equals(severity)) {
            log.info("告警级别 {} 不触发外部通知：rule={} subject={}", severity, ruleCode, subject);
            return;
        }

        String text = recovery
                ? "[已恢复] " + ruleCode + " · " + subject + "\n" + safe(detail)
                : "[" + severity + "] " + ruleCode + " · " + subject + "\n" + safe(detail);
        String usageKey = recovery ? "ALERT_RECOVERY" : "ALERT_FATAL";

        attempt("alert_im", usageKey, null, text, sent, failures);
        if (!recovery) {
            attempt("sms", "ALERT_FATAL", routeRecipient("sms", "ALERT_FATAL"), text, sent, failures);
            attempt("email", "ALERT_FATAL", routeRecipient("email", "ALERT_FATAL"), text, sent, failures);
        }

        boolean alerted = !sent.isEmpty();
        String error = failures.isEmpty() ? null : String.join(" | ", failures);
        jdbc.update("""
                UPDATE risk_alert SET alerted = ?, alerted_at = ?, alert_channel = ?, alert_error = ?
                 WHERE id = ?
                """, alerted, alerted ? OffsetDateTime.now(ZoneOffset.UTC) : null,
                sent.isEmpty() ? null : String.join(",", sent),
                error == null ? null : truncate(error), alertId);
        if (alerted) {
            log.warn("告警已派发 alertId={} channel={} rule={}", alertId, sent, ruleCode);
        } else {
            log.error("告警未能派发 alertId={} rule={} reason={}", alertId, ruleCode, error);
        }
    }

    private void attempt(String capability, String usageKey, String recipient, String text,
                         List<String> sent, List<String> failures) {
        try {
            var result = channels.send(capability, usageKey, recipient, "告警通知", text);
            if (result.mock()) {
                failures.add(capability + "/" + usageKey + ": 演示模式未真正发送");
                return;
            }
            sent.add(capability + "/" + result.provider());
        } catch (RuntimeException error) {
            failures.add(capability + "/" + usageKey + ": " + error.getMessage());
        }
    }

    private String routeRecipient(String capability, String usageKey) {
        return routes.find(capability, usageKey).map(IntegrationRouteService.Resolved::recipient)
                .orElse(null);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value) {
        return value.length() <= 512 ? value : value.substring(0, 509) + "...";
    }
}
