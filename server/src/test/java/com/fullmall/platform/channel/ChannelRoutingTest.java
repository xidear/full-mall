package com.fullmall.platform.channel;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.risk.AlertDispatchService;
import com.fullmall.platform.risk.RiskAlertService;
import com.fullmall.platform.secret.IntegrationConfigService;
import com.sun.net.httpserver.HttpServer;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 通道路由端到端（真库 + 官方协议桩）：
 * 供应商并存 → 用途绑定 → 发送走绑定 → 告警派发与恢复。
 *
 * <p>断言的是「配置改了不用改代码」：同一能力挂两家，只有绑定的那家会被调用；
 * 没绑定就必须失败，不允许偷偷回退到「第一个启用的供应商」。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
                + ",org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@ActiveProfiles("test")
class ChannelRoutingTest {

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

    @AfterAll
    static void stopPostgres() throws IOException {
        POSTGRES.close();
    }

    @Autowired
    IntegrationConfigService configs;
    @Autowired
    IntegrationRouteService routes;
    @Autowired
    ChannelSendService channels;
    @Autowired
    com.fullmall.notification.app.NotificationService notifications;
    @Autowired
    RiskAlertService riskAlerts;
    @Autowired
    AlertDispatchService dispatch;
    @Autowired
    JdbcTemplate jdbc;

    private HttpServer stub;
    private final List<String> stubBodies = new CopyOnWriteArrayList<>();

    @BeforeEach
    void reset() throws IOException {
        jdbc.update("DELETE FROM integration_route");
        jdbc.update("DELETE FROM integration_config");
        jdbc.update("DELETE FROM risk_alert");
        if (stub != null) {
            stub.stop(0);
        }
        stubBodies.clear();
        stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stub.createContext("/hook", exchange -> {
            stubBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] payload = "{\"errcode\":0,\"errmsg\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        stub.start();
    }

    private String stubUrl() {
        return "http://127.0.0.1:" + stub.getAddress().getPort() + "/hook";
    }

    @Test
    void everyUsageStartsUnbound() {
        var rows = routes.matrix();

        assertThat(rows).isNotEmpty();
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.provider()).isNull();
            assertThat(row.availableProviders()).isEmpty();
        });
    }

    @Test
    void multipleProvidersOfSameCapabilityCanBeEnabledTogether() {
        // 这是「验证码走阿里、告警走腾讯」的前提：老实现启用一家会停用另一家。
        configs.save("email", "126", Map.of("username", "shop@126.com", "password", "authcode"), true, "tester");
        configs.save("email", "mock", Map.of(), true, "tester");

        assertThat(configs.enabledProviders("email")).containsExactlyInAnyOrder("126", "mock");
    }

    @Test
    void bindingRejectsUnknownUsage() {
        assertThatThrownBy(() -> routes.bind("sms", "NO_SUCH_USAGE", "mock", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("未登记的通信用途");
    }

    @Test
    void bindingRejectsProviderWithoutCredentials() {
        assertThatThrownBy(() -> routes.bind("sms", "OTP_LOGIN", "aliyun", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("尚未在后台配置");
    }

    @Test
    void alertUsageRequiresRecipient() {
        configs.save("email", "mock", Map.of(), true, "tester");

        assertThatThrownBy(() -> routes.bind("email", "ALERT_FATAL", "mock", null))
                .hasMessageContaining("必须填写收件人");

        var bound = routes.bind("email", "ALERT_FATAL", "mock", "oncall@example.com");
        assertThat(bound.recipient()).isEqualTo("oncall@example.com");
    }

    @Test
    void unboundUsageFailsInsteadOfPickingFirstEnabledProvider() {
        configs.save("email", "mock", Map.of(), true, "tester");

        assertThatThrownBy(() -> channels.send("email", "OTP_LOGIN", "buyer@example.com", "标题", "内容"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("尚未绑定通道");
    }

    @Test
    void sendingFollowsTheBoundProviderNotTheFirstEnabledOne() {
        configs.save("email", "126", Map.of("username", "shop@126.com", "password", "authcode"), true, "tester");
        configs.save("email", "mock", Map.of(), true, "tester");
        routes.bind("email", "OTP_LOGIN", "mock", null);

        var result = channels.send("email", "OTP_LOGIN", "buyer@example.com", "标题", "内容");

        // 若回退到「第一个启用的供应商」这里会去调阿里云真实接口而失败
        assertThat(result.provider()).isEqualTo("mock");
        assertThat(result.mock()).isTrue();
    }

    @Test
    void notificationDeliveryIsFailedWithReasonWhenRouteMissing() {
        var sent = notifications.sendTest("SMS", "标题", "内容", "EMAIL_OTP");

        assertThat(sent.status()).isEqualTo("FAILED");
        assertThat(sent.errorMessage()).contains("尚未绑定通道");
    }

    @Test
    void alertIsDispatchedToBoundRobotAndRecorded() {
        configs.save("alert_im", "wecom", Map.of("webhook-url", stubUrl()), true, "tester");
        routes.bind("alert_im", "ALERT_FATAL", "wecom", null);

        riskAlerts.raise("PAYMENT_CALLBACK_LOST", RiskAlertService.Severity.FATAL,
                "order-1001", "微信支付回调超时未确认", Duration.ofMinutes(1));
        UUID id = jdbc.queryForObject("SELECT id FROM risk_alert WHERE rule_code = ?", UUID.class,
                "PAYMENT_CALLBACK_LOST");

        dispatch.dispatchNow(id, false);

        assertThat(stubBodies).isNotEmpty();
        assertThat(stubBodies).anySatisfy(body -> {
            assertThat(body).contains("PAYMENT_CALLBACK_LOST");
            assertThat(body).contains("order-1001");
        });
        var view = riskAlerts.findById(id);
        assertThat(view.alerted()).isTrue();
        assertThat(view.alertChannel()).contains("alert_im/wecom");
        assertThat(view.alertedAt()).isNotNull();
    }

    /**
     * 站内信是默认通道（2026-11-05 改造）：没绑群机器人也照样算「已推送」（IN_APP），
     * 但外部通道为什么没发出去仍必须写进 alertError——「站内信到了」不能掩盖「没人被叫醒」。
     */
    @Test
    void alertWithoutBoundRobotStillDeliversInAppAndRecordsWhyExternalFailed() {
        riskAlerts.raise("STOCK_RECONCILE_FAILED", RiskAlertService.Severity.FATAL,
                "sku-1", "库存对账不平", Duration.ofMinutes(1));
        UUID id = jdbc.queryForObject("SELECT id FROM risk_alert WHERE rule_code = ?", UUID.class,
                "STOCK_RECONCILE_FAILED");

        dispatch.dispatchNow(id, false);

        var view = riskAlerts.findById(id);
        assertThat(view.alerted()).isTrue();
        assertThat(view.alertChannel()).contains("IN_APP");
        assertThat(view.alertChannel()).doesNotContain("alert_im");
        assertThat(view.alertError()).contains("尚未绑定通道");
    }

    @Test
    void mediumSeverityDoesNotPageAnyone() {
        configs.save("alert_im", "wecom", Map.of("webhook-url", stubUrl()), true, "tester");
        routes.bind("alert_im", "ALERT_FATAL", "wecom", null);

        riskAlerts.raise("EXPORT_SCOPE_DENIED", RiskAlertService.Severity.MEDIUM,
                "supplier-a", "越权访问导出", Duration.ofMinutes(1));
        UUID id = jdbc.queryForObject("SELECT id FROM risk_alert WHERE rule_code = ?", UUID.class,
                "EXPORT_SCOPE_DENIED");

        dispatch.dispatchNow(id, false);

        assertThat(stubBodies).isEmpty();
        assertThat(riskAlerts.findById(id).alerted()).isFalse();
    }

    @Test
    void recoverySendsRecoveryNoticeAndMarksAlert() {
        configs.save("alert_im", "wecom", Map.of("webhook-url", stubUrl()), true, "tester");
        routes.bind("alert_im", "ALERT_RECOVERY", "wecom", null);
        riskAlerts.raise("PAYMENT_CALLBACK_LOST", RiskAlertService.Severity.FATAL,
                "order-2002", "回调丢失", Duration.ofMinutes(1));
        UUID id = jdbc.queryForObject("SELECT id FROM risk_alert WHERE rule_code = ?", UUID.class,
                "PAYMENT_CALLBACK_LOST");

        riskAlerts.recover(id);
        dispatch.dispatchNow(id, true);

        assertThat(stubBodies).anySatisfy(body -> assertThat(body).contains("已恢复"));
        assertThat(riskAlerts.findById(id).recoveredAt()).isNotNull();
    }

    @Test
    void unbindMakesUsageUnavailableAgain() {
        configs.save("email", "mock", Map.of(), true, "tester");
        routes.bind("email", "OTP_LOGIN", "mock", null);
        routes.unbind("email", "OTP_LOGIN");

        assertThatThrownBy(() -> channels.send("email", "OTP_LOGIN", "buyer@example.com", "标题", "内容"))
                .hasMessageContaining("尚未绑定通道");
        assertThatThrownBy(() -> routes.unbind("sms", "OTP_LOGIN"))
                .hasMessageContaining("当前未绑定");
    }
}
