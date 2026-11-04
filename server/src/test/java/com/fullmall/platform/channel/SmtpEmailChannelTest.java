package com.fullmall.platform.channel;

import com.fullmall.platform.channel.sender.ChannelSettings;
import com.fullmall.platform.channel.sender.SmtpEmailChannel;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 邮件通道（GreenMail 进程内 SMTP，**不带真实邮箱凭据**）：验证邮件真的发出去了，
 * 且发件人/主题/正文符合契约。自建 SMTP 预设的报文路径与 126/QQ 完全一致（只差预设的 host/port）。
 */
class SmtpEmailChannelTest {

    private GreenMail greenMail;

    @BeforeEach
    void startSmtp() {
        greenMail = new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
        // 桩服务器默认要求认证：不建用户会得到 535，看起来像「我们的发送逻辑错了」。
        greenMail.setUser("shop@example.com", "shop@example.com", "authcode");
        greenMail.start();
    }

    @AfterEach
    void stopSmtp() {
        greenMail.stop();
    }

    @Test
    void sendsMessageThroughSmtp() throws Exception {
        SmtpEmailChannel channel = new SmtpEmailChannel("smtp", new ChannelSettings(Map.of(
                "host", "127.0.0.1",
                "port", String.valueOf(greenMail.getSmtp().getPort()),
                "username", "shop@example.com",
                "password", "authcode",
                "from-name", "示例商城")));

        channel.send("buyer@example.com", "登录验证码", "您的登录验证码是 123456");

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo("登录验证码");
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("buyer@example.com");
        assertThat(String.valueOf(received[0].getContent())).contains("123456");
    }

    @Test
    void invalidPortIsRejectedBeforeContactingServer() {
        assertThatThrownBy(() -> new SmtpEmailChannel("smtp", new ChannelSettings(Map.of(
                "host", "127.0.0.1", "port", "abc",
                "username", "shop@example.com", "password", "authcode"))))
                .hasMessageContaining("SMTP 端口必须是数字");
    }

    @Test
    void smtpFailureSurfacesAsException() {
        SmtpEmailChannel channel = new SmtpEmailChannel("smtp", new ChannelSettings(Map.of(
                "host", "127.0.0.1",
                "port", String.valueOf(greenMail.getSmtp().getPort()),
                "username", "shop@example.com", "password", "authcode")));
        greenMail.stop();

        assertThatThrownBy(() -> channel.send("buyer@example.com", "主题", "正文"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("发送邮件失败");
    }
}
