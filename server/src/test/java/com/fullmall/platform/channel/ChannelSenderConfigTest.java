package com.fullmall.platform.channel;

import com.fullmall.platform.channel.sender.AliyunSmsChannel;
import com.fullmall.platform.channel.sender.ChannelSettings;
import com.fullmall.platform.channel.sender.ImWebhookChannel;
import com.fullmall.platform.channel.sender.SmtpEmailChannel;
import com.fullmall.platform.channel.sender.TencentSmsChannel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 通道实现的配置契约：缺必填项必须**构造即失败**并说清缺哪一项，
 * 不允许等到发送时才暴露、更不允许带着空凭据发出去后把错误记成「供应商鉴权失败」。
 */
class ChannelSenderConfigTest {

    @Test
    void smsChannelsFailFastWithMissingCredentials() {
        assertThatThrownBy(() -> new AliyunSmsChannel(new ChannelSettings(Map.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AccessKey ID");
        assertThatThrownBy(() -> new AliyunSmsChannel(new ChannelSettings(Map.of(
                "access-key-id", "ak", "access-key-secret", "sk", "sign-name", "签名"))))
                .hasMessageContaining("短信模板 CODE");
        assertThatThrownBy(() -> new TencentSmsChannel(new ChannelSettings(Map.of("secret-id", "id"))))
                .hasMessageContaining("SecretKey");
        assertThatThrownBy(() -> new TencentSmsChannel(new ChannelSettings(Map.of(
                "secret-id", "id", "secret-key", "sk", "sdk-app-id", "1400",
                "sign-name", "签名"))))
                .hasMessageContaining("模板 ID");
    }

    @Test
    void smsChannelsAcceptCompleteCredentials() {
        assertThatCode(() -> new AliyunSmsChannel(new ChannelSettings(Map.of(
                "access-key-id", "ak", "access-key-secret", "sk",
                "sign-name", "签名", "template-code", "SMS_1"))))
                .doesNotThrowAnyException();
        assertThatCode(() -> new TencentSmsChannel(new ChannelSettings(Map.of(
                "secret-id", "id", "secret-key", "sk", "sdk-app-id", "1400",
                "sign-name", "签名", "template-id", "1234"))))
                .doesNotThrowAnyException();
    }

    @Test
    void emailPresetsNeedOnlyMailboxAndAuthCode() {
        // 126/QQ 是预设：运营不该被要求填写 smtp.126.com:465，填错一个字符就是死配置。
        assertThatCode(() -> new SmtpEmailChannel("126", new ChannelSettings(Map.of(
                "username", "shop@126.com", "password", "authcode"))))
                .doesNotThrowAnyException();
        assertThatCode(() -> new SmtpEmailChannel("qq", new ChannelSettings(Map.of(
                "username", "shop@qq.com", "password", "authcode"))))
                .doesNotThrowAnyException();
        // 自建 SMTP 必须显式给地址与端口
        assertThatThrownBy(() -> new SmtpEmailChannel("smtp", new ChannelSettings(Map.of(
                "username", "a@b.c", "password", "x"))))
                .hasMessageContaining("SMTP 服务器地址");
        assertThatThrownBy(() -> new SmtpEmailChannel("unknown", new ChannelSettings(Map.of())))
                .hasMessageContaining("不支持的邮件供应商");
    }

    @Test
    void imChannelValidatesProviderAndWebhook() {
        assertThatThrownBy(() -> new ImWebhookChannel("unknown",
                new ChannelSettings(Map.of("webhook-url", "https://example.com/hook"))))
                .hasMessageContaining("不支持的告警群机器人供应商");
        assertThatThrownBy(() -> new ImWebhookChannel("wecom",
                new ChannelSettings(Map.of("webhook-url", "ftp://example.com/hook"))))
                .hasMessageContaining("http(s)://");
        assertThatThrownBy(() -> new ImWebhookChannel("wecom", new ChannelSettings(Map.of())))
                .hasMessageContaining("Webhook 地址");
    }

    @Test
    void eventTypeToUsageMappingIsExplicit() {
        assertThat(ChannelSendService.usageFor("sms", "EMAIL_OTP")).isEqualTo("OTP_LOGIN");
        assertThat(ChannelSendService.usageFor("sms", "ORDER_PAID")).isEqualTo("ORDER_EVENT");
        assertThat(ChannelSendService.usageFor("email", "PICKUP_READY")).isEqualTo("ORDER_EVENT");
        assertThat(ChannelSendService.usageFor("alert_im", "ALERT_FATAL")).isEqualTo("ALERT_FATAL");
        assertThat(ChannelSendService.usageFor("alert_im", "ALERT_RECOVERY")).isEqualTo("ALERT_RECOVERY");
        // 未登记的事件必须显式失败：悄悄落到默认用途会把告警当订单通知发出去
        assertThatThrownBy(() -> ChannelSendService.usageFor("sms", "SOMETHING_NEW"))
                .hasMessageContaining("没有对应的通信用途");
    }
}
