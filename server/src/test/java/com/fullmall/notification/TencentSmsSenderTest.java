package com.fullmall.notification;

import com.fullmall.notification.channel.tencent.TencentSmsSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 腾讯云短信：缺配置启动即失败；channel 为 SMS；适配器不泄漏到领域层。 */
class TencentSmsSenderTest {

    @Test
    void missingConfigFailsFast() {
        assertThatThrownBy(() -> new TencentSmsSender("", "sk", "ap-guangzhou", "1400", "签名", "tpl"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("必须配置");
        assertThatThrownBy(() -> new TencentSmsSender("id", "sk", "ap-guangzhou", "", "签名", "tpl"))
                .hasMessageContaining("必须配置");
        assertThatThrownBy(() -> new TencentSmsSender("id", "sk", "ap-guangzhou", "1400", "", "tpl"))
                .hasMessageContaining("必须配置");
    }

    @Test
    void channelIsSmsAndClassIsIsolatedInPackage() {
        var sender = new TencentSmsSender("id", "sk", "ap-guangzhou", "1400", "签名", "tpl");

        assertThat(sender.channel()).isEqualTo("SMS");
        assertThat(sender.getClass().getPackageName())
                .as("适配器必须自成一个可删除的包")
                .isEqualTo("com.fullmall.notification.channel.tencent");
        assertThat(com.fullmall.notification.channel.NotificationChannelSender.class
                .isAssignableFrom(sender.getClass())).isTrue();
    }
}
