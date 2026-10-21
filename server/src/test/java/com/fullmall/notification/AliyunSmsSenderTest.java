package com.fullmall.notification;

import com.fullmall.notification.channel.AliyunSmsSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 阿里云短信适配：配置缺失必须**启动期失败**（不允许发送时才暴露），
 * 且 channel 固定为 SMS（否则通知路由会错）。
 */
class AliyunSmsSenderTest {

    @Test
    void missingConfigFailsFast() {
        assertThatThrownBy(() -> new AliyunSmsSender("", "secret", "", "签名", "SMS_1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("必须配置");
        assertThatThrownBy(() -> new AliyunSmsSender("ak", "secret", "", "", "SMS_1"))
                .hasMessageContaining("必须配置");
        assertThatThrownBy(() -> new AliyunSmsSender("ak", "secret", "", "签名", ""))
                .hasMessageContaining("必须配置");
    }

    @Test
    void channelIsSmsWhenConfigured() {
        AliyunSmsSender sender = new AliyunSmsSender("ak", "secret", "", "签名", "SMS_1");
        assertThat(sender.channel()).isEqualTo("SMS");
    }
}
