package com.fullmall.notification.channel;

import com.fullmall.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockNotificationChannelSender implements NotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationChannelSender.class);

    private final String channel;

    public MockNotificationChannelSender(String channel) {
        this.channel = channel;
    }

    @Override
    public String channel() {
        return channel;
    }

    @Override
    public void send(Notification notification) {
        log.info("[MOCK][{}] to={} event={} title={}",
                channel,
                mask(notification.recipientId()),
                notification.eventType(),
                notification.title());
    }

    private String mask(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}