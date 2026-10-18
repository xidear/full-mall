package com.fullmall.notification.channel;

import com.fullmall.notification.domain.Notification;

public interface NotificationChannelSender {

    String channel();

    void send(Notification notification);
}