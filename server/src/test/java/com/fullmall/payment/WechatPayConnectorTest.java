package com.fullmall.payment;

import com.fullmall.payment.app.WechatPayConnector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 微信支付：缺配置启动失败；已配置但 API 未接入时**明确拒绝**，不回落 mock。 */
class WechatPayConnectorTest {

    @Test
    void missingConfigFailsFast() {
        assertThatThrownBy(() -> new WechatPayConnector("", "serial", "key", "/tmp/k.pem"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("必须配置");
        assertThatThrownBy(() -> new WechatPayConnector("mch", "serial", "", "/tmp/k.pem"))
                .hasMessageContaining("必须配置");
        assertThatThrownBy(() -> new WechatPayConnector("mch", "serial", "key", ""))
                .hasMessageContaining("必须配置");
    }

    @Test
    void configuredButNotIntegratedIsRejectedExplicitly() {
        WechatPayConnector connector = new WechatPayConnector("mch", "serial", "key", "/tmp/k.pem");

        assertThat(connector.provider()).isEqualTo("WECHAT");
        assertThatThrownBy(() -> connector.createPayment(null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("尚未接入");
        assertThatThrownBy(() -> connector.refund(null, 100))
                .hasMessageContaining("尚未接入");
    }
}
