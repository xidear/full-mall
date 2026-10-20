package com.fullmall.notification.config;

import com.fullmall.notification.channel.NotificationChannelSender;
import com.fullmall.notification.domain.Notification;

/**
 * 未配置真实通道时的占位 sender：**投递必然失败**。
 *
 * <p>为什么不用 mock 顶上：mock 会把状态写成 `SENT`，运营看到"已发送"却没人收到。
 * 这里抛出明确异常，通知记录落 `FAILED` 并带原因，运维一眼能看出来。
 */
public class UnconfiguredChannelSender implements NotificationChannelSender {

    private final String channel;
    private final String provider;

    public UnconfiguredChannelSender(String channel, String provider) {
        this.channel = channel;
        this.provider = provider;
    }

    @Override
    public String channel() {
        return channel;
    }

    @Override
    public void send(Notification notification) {
        throw new IllegalStateException(
                "通知渠道 " + channel + " 未配置真实服务商（当前 provider=" + provider + "），拒绝伪造发送成功");
    }
}
