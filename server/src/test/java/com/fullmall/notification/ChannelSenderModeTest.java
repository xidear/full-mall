package com.fullmall.notification;

import com.fullmall.notification.channel.NotificationChannelSender;
import com.fullmall.notification.config.NotificationChannelConfig;
import com.fullmall.notification.domain.Notification;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 演示/生产隔离：生产环境绝不注册 mock 外部渠道 sender。 */
class ChannelSenderModeTest {

    private static Notification sample() {
        return new Notification(UUID.randomUUID(), "MEMBER", "m-1", "ORDER_PAID", "SMS",
                "标题", "内容", "PENDING", null, null, null, Instant.now());
    }

    @Test
    void demoModeUsesMockSenders() {
        var config = new NotificationChannelConfig("demo");
        NotificationChannelSender sms = config.smsNotificationSender("mock", "", "", "", "", "");
        assertThat(sms.channel()).isEqualTo("SMS");
        sms.send(sample());   // mock 不抛错
    }

    @Test
    void productionModeNeverFakesExternalDelivery() {
        var config = new NotificationChannelConfig("prod");
        NotificationChannelSender sms = config.smsNotificationSender("mock", "", "", "", "", "");

        // 关键断言：生产下 mock 也不会被当成成功，而是明确失败
        assertThatThrownBy(() -> sms.send(sample()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("拒绝伪造发送成功");
    }

    @Test
    void productionModeReportsUnconfiguredRealProviderExplicitly() {
        var config = new NotificationChannelConfig("prod");
        // 真实通道缺配置：**启动期**就失败（比"发送时才发现"更早暴露）
        assertThatThrownBy(() -> config.smsNotificationSender("aliyun", "", "", "", "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("必须配置");

        // 其它未接入渠道仍是"总是失败"的 sender，不允许伪装成功
        NotificationChannelSender email = config.emailNotificationSender("aliyun");
        assertThatThrownBy(() -> email.send(sample()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未配置真实服务商");
    }

    @Test
    void inAppChannelIsAlwaysAvailable() {
        var config = new NotificationChannelConfig("prod");
        config.inAppNotificationSender().send(sample());   // 站内消息落库即可，不需要外部服务
    }
}
