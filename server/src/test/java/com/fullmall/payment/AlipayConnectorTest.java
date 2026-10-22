package com.fullmall.payment;

import com.fullmall.payment.app.AlipayConnector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 支付宝：缺配置启动失败；未接入时明确拒绝，不回落 mock。 */
class AlipayConnectorTest {

    @Test
    void missingConfigFailsFast() {
        assertThatThrownBy(() -> new AlipayConnector("", "priv", "pub", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("必须配置");
        assertThatThrownBy(() -> new AlipayConnector("app", "", "pub", null))
                .hasMessageContaining("必须配置");
        assertThatThrownBy(() -> new AlipayConnector("app", "priv", "", null))
                .hasMessageContaining("必须配置");
    }

    @Test
    void configuredButNotIntegratedIsRejected() {
        AlipayConnector connector = new AlipayConnector("app", "priv", "pub", null);

        assertThat(connector.provider()).isEqualTo("ALIPAY");
        assertThatThrownBy(() -> connector.createPayment(null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("尚未接入");
    }
}
