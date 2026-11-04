package com.fullmall.notification;

import com.fullmall.notification.app.NotificationService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 渠道自检（ADR-0018 之后）：发测试消息走「用途路由」——
 * 没绑定就必须失败并说清原因，绑了 mock 才算发送成功（且标明未真实发出）。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class ChannelTestSendTest {

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
    NotificationService notifications;
    @Autowired
    com.fullmall.platform.secret.IntegrationConfigService configs;
    @Autowired
    com.fullmall.platform.channel.IntegrationRouteService routes;

    @Test
    void testMessageFailsWithReasonWhenNoRouteBound() {
        var sent = notifications.sendTest("sms", null, null, null);

        assertThat(sent.channel()).isEqualTo("SMS");
        assertThat(sent.eventType()).isEqualTo("CHANNEL_TEST");
        assertThat(sent.status()).isEqualTo("FAILED");
        assertThat(sent.errorMessage()).contains("尚未绑定通道");
    }

    @Test
    void testMessageGoesThroughBoundProvider() {
        configs.save("email", "mock", java.util.Map.of(), true, "tester");
        routes.bind("email", "ORDER_EVENT", "mock", null);

        var sent = notifications.sendTest("email", null, null, null);

        assertThat(sent.channel()).isEqualTo("EMAIL");
        assertThat(sent.status()).isEqualTo("SENT");
        assertThat(sent.errorMessage()).isNull();
    }

    @Test
    void rejectsUnknownChannel() {
        assertThatThrownBy(() -> notifications.sendTest("carrier-pigeon", null, null, null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("未知渠道", null);
    }
}
