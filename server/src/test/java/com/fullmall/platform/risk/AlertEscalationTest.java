package com.fullmall.platform.risk;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 告警派发与升级：站内信是默认通道、确认能停掉升级、超时才升级、级别有上限。
 *
 * <p>为什么要在真实数据库上验：这条链路的坑都在 SQL 与状态位（alerted/acknowledged_at/escalation_level），
 * 用 mock 的 JdbcTemplate 只能证明"调了某个方法"，证明不了"管理端真的看得到这条告警"。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.mode=demo"
        })
@ActiveProfiles("test")
class AlertEscalationTest {

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
    RiskAlertService alerts;

    @Autowired
    AlertDispatchService dispatch;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM notification");
        jdbc.update("DELETE FROM risk_alert");
        // 故意一条通道路由都不绑：站内信必须照样送到，这才叫「默认通道」
        jdbc.update("DELETE FROM integration_route");
    }

    private UUID raise(SeverityLike severity) {
        return alerts.raise("E2E_TEST_RULE", severity.severity, "subject-" + UUID.randomUUID(),
                "detail", Duration.ZERO) ? lastAlertId() : null;
    }

    /** raise() 只返回 boolean，这里按最新一条取 id（用例里每次只插一条）。 */
    private UUID lastAlertId() {
        return jdbc.queryForObject("SELECT id FROM risk_alert ORDER BY created_at DESC LIMIT 1",
                UUID.class);
    }

    private enum SeverityLike {
        FATAL(RiskAlertService.Severity.FATAL),
        HIGH(RiskAlertService.Severity.HIGH);

        private final RiskAlertService.Severity severity;

        SeverityLike(RiskAlertService.Severity severity) {
            this.severity = severity;
        }
    }

    /** 站内信是默认通道：没有任何外部通道配置时，告警也必须进通知中心并且算「已推送」。 */
    @Test
    void inAppIsDefaultChannelAndMarksAlerted() {
        UUID id = raise(SeverityLike.FATAL);
        dispatch.dispatchNow(id, false);

        List<String> channels = jdbc.queryForList(
                "SELECT channel FROM notification WHERE event_type = 'ALERT_FATAL'", String.class);
        assertThat(channels).contains("IN_APP");

        var row = alerts.findById(id);
        assertThat(row.alerted()).isTrue();
        assertThat(row.alertChannel()).contains("IN_APP");
        assertThat(row.alertedAt()).isNotNull();
    }

    /** 确认处理：记人、记时间，并把这条从升级候选里摘掉。 */
    @Test
    void acknowledgeStopsEscalation() {
        UUID id = raise(SeverityLike.FATAL);
        var acknowledged = alerts.acknowledge(id, "operatorA");
        assertThat(acknowledged.acknowledgedBy()).isEqualTo("operatorA");
        assertThat(acknowledged.acknowledgedAt()).isNotNull();

        assertThat(alerts.escalationCandidates(Instant.now().plus(Duration.ofDays(1)),
                Duration.ofMinutes(15), Duration.ofHours(1), 3)).isEmpty();

        assertThatThrownBy(() -> alerts.acknowledge(id, "operatorB"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("已被确认");
    }

    /** 升级间隔按严重度分档：FATAL 15 分钟内不升级，超过才升级；HIGH 要等满 1 小时。 */
    @Test
    void escalationRespectsPerSeverityInterval() {
        UUID fatal = raise(SeverityLike.FATAL);
        UUID high = raise(SeverityLike.HIGH);
        Instant now = Instant.now();

        assertThat(alerts.escalationCandidates(now.plus(Duration.ofMinutes(10)),
                Duration.ofMinutes(15), Duration.ofHours(1), 3)).isEmpty();

        List<RiskAlertService.RiskAlert> afterFatalInterval = alerts.escalationCandidates(
                now.plus(Duration.ofMinutes(16)), Duration.ofMinutes(15), Duration.ofHours(1), 3);
        assertThat(afterFatalInterval).extracting(RiskAlertService.RiskAlert::id).contains(fatal);
        assertThat(afterFatalInterval).extracting(RiskAlertService.RiskAlert::id).doesNotContain(high);

        List<RiskAlertService.RiskAlert> afterHighInterval = alerts.escalationCandidates(
                now.plus(Duration.ofMinutes(61)), Duration.ofMinutes(15), Duration.ofHours(1), 3);
        assertThat(afterHighInterval).extracting(RiskAlertService.RiskAlert::id).contains(fatal, high);
    }

    /** 升级会真的再推一条站内信，并且级别从 0 涨到 1；到达上限后不再出现在候选里。 */
    @Test
    void escalatePushesAnotherInAppMessageAndStopsAtMaxLevel() {
        UUID id = raise(SeverityLike.FATAL);
        dispatch.dispatchNow(id, false);

        var escalated = alerts.escalate(id);
        assertThat(escalated.escalationLevel()).isEqualTo(1);
        assertThat(escalated.escalatedAt()).isNotNull();

        List<String> contents = jdbc.queryForList(
                "SELECT content FROM notification WHERE event_type = 'ALERT_FATAL'", String.class);
        assertThat(contents).anyMatch(text -> text.contains("[升级 L1]"));

        assertThat(alerts.escalationCandidates(Instant.now().plus(Duration.ofDays(1)),
                Duration.ofMinutes(15), Duration.ofHours(1), 1)).isEmpty();
    }
}
