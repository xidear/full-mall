package com.fullmall.notification;

import com.fullmall.notification.app.NotificationService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class NotificationRecipientTest {

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

    private UUID notificationId;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM notification WHERE recipient_id IN ('member-a', 'member-b')");
        notificationId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO notification (id, recipient_type, recipient_id, event_type, channel,
                                          title, content, status)
                VALUES (?, 'MEMBER', 'member-a', 'ORDER_PAID', 'IN_APP', '订单已支付', '内容', 'SENT')
                """, notificationId);
        jdbc.update("""
                INSERT INTO notification (id, recipient_type, recipient_id, event_type, channel,
                                          title, content, status)
                VALUES (?, 'MEMBER', 'member-b', 'ORDER_PAID', 'IN_APP', '别人的消息', '内容', 'SENT')
                """, UUID.randomUUID());
    }

    @Test
    void notifyMemberPersistsInAppMessage() {
        notifications.notifyMember("member-c", "ORDER_PAID", "支付成功", "内容");

        var mine = notifications.findForRecipient("MEMBER", "member-c", 10);
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).eventType()).isEqualTo("ORDER_PAID");
        assertThat(mine.get(0).channel()).isEqualTo("IN_APP");
    }

    @Test
    void purgesOnlyOldMessages() {
        UUID oldId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO notification (id, recipient_type, recipient_id, event_type, channel,
                                          title, content, status, created_at)
                VALUES (?, 'MEMBER', 'member-old', 'ORDER_PAID', 'IN_APP', '旧消息', '内容', 'SENT',
                        now() - interval '200 days')
                """, oldId);
        jdbc.update("""
                INSERT INTO notification (id, recipient_type, recipient_id, event_type, channel,
                                          title, content, status)
                VALUES (?, 'MEMBER', 'member-new', 'ORDER_PAID', 'IN_APP', '新消息', '内容', 'SENT')
                """, UUID.randomUUID());

        int removed = notifications.purgeOlderThan(180);

        assertThat(removed).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE id = ?", Integer.class, oldId)).isZero();
        assertThat(notifications.findForRecipient("MEMBER", "member-new", 10)).hasSize(1);
    }

    @Test
    void purgeRejectsNonPositiveDays() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> notifications.purgeOlderThan(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("保留天数");
    }

    @Test
    void memberSeesOnlyOwnMessages() {
        var mine = notifications.findForRecipient("MEMBER", "member-a", 10);

        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).title()).isEqualTo("订单已支付");
    }

    @Test
    void unreadCountAndMarkReadAreScopedToRecipient() {
        assertThat(notifications.unreadCount("MEMBER", "member-a")).isEqualTo(1);

        // 别的会员不能把我的消息标记已读
        assertThat(notifications.markRead("MEMBER", "member-b", notificationId)).isFalse();
        assertThat(notifications.unreadCount("MEMBER", "member-a")).isEqualTo(1);

        assertThat(notifications.markRead("MEMBER", "member-a", notificationId)).isTrue();
        assertThat(notifications.unreadCount("MEMBER", "member-a")).isZero();
        // 重复标记不报错但返回 false
        assertThat(notifications.markRead("MEMBER", "member-a", notificationId)).isFalse();
    }
}
