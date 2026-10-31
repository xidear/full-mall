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

/** 渠道自检：mock 渠道返回 SENT；未知渠道明确拒绝。 */
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

    @Test
    void sendsTestMessageAndReportsStatus() {
        var sent = notifications.sendTest("sms", null, null, null);

        assertThat(sent.channel()).isEqualTo("SMS");
        assertThat(sent.eventType()).isEqualTo("CHANNEL_TEST");
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
