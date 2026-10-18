package com.fullmall.notification.config;

import com.fullmall.notification.channel.MockNotificationChannelSender;
import com.fullmall.notification.channel.NotificationChannelSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationChannelConfig {

    @Bean
    public NotificationChannelSender inAppNotificationSender() {
        return new MockNotificationChannelSender("IN_APP");
    }

    @Bean
    public NotificationChannelSender smsNotificationSender() {
        return new MockNotificationChannelSender("SMS");
    }

    @Bean
    public NotificationChannelSender emailNotificationSender() {
        return new MockNotificationChannelSender("EMAIL");
    }

    @Bean
    public NotificationChannelSender wechatNotificationSender() {
        return new MockNotificationChannelSender("WECHAT");
    }

    @Bean
    public NotificationChannelSender appPushNotificationSender() {
        return new MockNotificationChannelSender("APP");
    }
}