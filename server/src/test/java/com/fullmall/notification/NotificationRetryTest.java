package com.fullmall.notification;

import com.fullmall.notification.app.NotificationService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 通知重试：仅 FAILED 可重试；已 SENT 拒绝（避免重复给顾客发短信）。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class NotificationRetryTest {

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
    JdbcTemplate jdbc;
    @Autowired
    com.fullmall.platform.secret.IntegrationConfigService configs;
    @Autowired
    com.fullmall.platform.channel.IntegrationRouteService routes;

    @org.junit.jupiter.api.BeforeEach
    void bindMockEmailRoute() {
        // 重试必须走已绑定的通道；未绑定时 retry 会如实再次失败（见 ChannelRoutingTest）。
        configs.save("email", "mock", java.util.Map.of(), true, "tester");
        routes.bind("email", "ORDER_EVENT", "mock", null);
    }

    private UUID failed(String channel) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO notification (id, recipient_type, recipient_id, event_type, channel,
                                          title, content, status, error_message)
                VALUES (?, 'STAFF', 'ALL', 'CHANNEL_TEST', ?, '标题', '内容', 'FAILED', '之前失败')
                """, id, channel);
        return id;
    }

    @Test
    void retriesFailedNotificationToSent() {
        UUID id = failed("EMAIL");

        var retried = notifications.retry(id);

        assertThat(retried.status()).isEqualTo("SENT");
        assertThat(retried.errorMessage()).isNull();
    }

    @Test
    void refusesToRetryAlreadySent() {
        UUID id = failed("EMAIL");
        notifications.retry(id);

        assertThatThrownBy(() -> notifications.retry(id))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("只有 FAILED 的通知可以重试");
    }
}
