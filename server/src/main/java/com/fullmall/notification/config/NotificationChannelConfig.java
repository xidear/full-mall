package com.fullmall.notification.config;

import com.fullmall.notification.channel.MockNotificationChannelSender;
import com.fullmall.notification.channel.NotificationChannelSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * 通知渠道装配。
 *
 * <p>**mock 只允许在非生产模式存在**：演示/开发用 mock 记录报文，
 * 生产环境下 **不注册任何 mock sender**——没有真实 sender 的渠道会在投递时
 * 明确记为 `FAILED`（"No sender configured"），而不是伪装成功。
 *
 * <p>`IN_APP` 是站内消息，落库即送达，不需要外部服务，因此任何模式都保留。
 */
@Configuration
public class NotificationChannelConfig {

    private final boolean production;

    public NotificationChannelConfig(@Value("${app.mode:dev}") String mode) {
        this.production = "prod".equals(mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT));
    }

    @Bean
    public NotificationChannelSender inAppNotificationSender() {
        return new MockNotificationChannelSender("IN_APP");
    }

    // 外部渠道：只有非生产模式才装 mock，避免"假发送成功"污染生产数据。
    @Bean
    public NotificationChannelSender smsNotificationSender(
            @Value("${app.sms.provider:mock}") String provider,
            @Value("${app.sms.aliyun.access-key-id:}") String aliyunKeyId,
            @Value("${app.sms.aliyun.access-key-secret:}") String aliyunKeySecret,
            @Value("${app.sms.aliyun.endpoint:}") String aliyunEndpoint,
            @Value("${app.sms.aliyun.sign-name:}") String aliyunSignName,
            @Value("${app.sms.aliyun.template-code:}") String aliyunTemplateCode) {
        if ("aliyun".equalsIgnoreCase(provider == null ? "" : provider.trim())) {
            // 真实通道：配置缺失会在这里启动失败，不会假装发送成功
            return new com.fullmall.notification.channel.AliyunSmsSender(
                    aliyunKeyId, aliyunKeySecret, aliyunEndpoint, aliyunSignName, aliyunTemplateCode);
        }
        return externalSender("SMS", provider);
    }

    @Bean
    public NotificationChannelSender emailNotificationSender(
            @Value("${app.sms.provider:mock}") String provider) {
        return externalSender("EMAIL", provider);
    }

    @Bean
    public NotificationChannelSender wechatNotificationSender(
            @Value("${app.sms.provider:mock}") String provider) {
        return externalSender("WECHAT", provider);
    }

    @Bean
    public NotificationChannelSender appPushNotificationSender(
            @Value("${app.sms.provider:mock}") String provider) {
        return externalSender("APP", provider);
    }

    private NotificationChannelSender externalSender(String channel, String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (!"mock".equals(normalized)) {
            // 真实渠道尚未接入 SDK：注册一个"总是失败"的 sender，保证不会伪装成功
            return new UnconfiguredChannelSender(channel, provider);
        }
        if (production) {
            return new UnconfiguredChannelSender(channel, "mock(production-disabled)");
        }
        return new MockNotificationChannelSender(channel);
    }
}
