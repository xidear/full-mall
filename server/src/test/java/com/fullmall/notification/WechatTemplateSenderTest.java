package com.fullmall.notification;

import com.fullmall.notification.channel.WechatTemplateSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 微信模板消息（WxJava SDK）：缺配置必须启动期失败，channel 固定 WECHAT。
 *
 * <p>真实发送需要公众号 appId/secret 与已审核模板，本仓库无凭据；
 * SDK 的发送路径由 WxJava 自身保证，我们不重复实现也不伪造成功。
 */
class WechatTemplateSenderTest {

    @Test
    void missingConfigFailsFast() {
        assertThatThrownBy(() -> new WechatTemplateSender("", "secret", "tpl"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("需配置");
        assertThatThrownBy(() -> new WechatTemplateSender("app", "", "tpl"))
                .hasMessageContaining("需配置");
        assertThatThrownBy(() -> new WechatTemplateSender("app", "secret", ""))
                .hasMessageContaining("需配置");
    }

    @Test
    void channelIsWechatWhenConfigured() {
        assertThat(new WechatTemplateSender("app", "secret", "tpl").channel()).isEqualTo("WECHAT");
    }
}
