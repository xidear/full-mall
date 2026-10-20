package com.fullmall.platform.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 演示/生产严格隔离：生产模式下任何 mock 适配器都必须启动失败。 */
class ProductionSafetyGuardTest {

    @Test
    void productionRejectsMockAdapters() {
        assertThatThrownBy(() -> new ProductionSafetyGuard("prod", "mock", "mock", "mock", "local"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("生产禁止 mock")
                .hasMessageContaining("app.payment.provider")
                .hasMessageContaining("app.logistics.provider")
                .hasMessageContaining("app.sms.provider");
    }

    @Test
    void productionAcceptsRealProviders() {
        assertThatCode(() -> new ProductionSafetyGuard(
                "prod", "wechat", "kuaidi100", "aliyun", "s3")).doesNotThrowAnyException();
    }

    @Test
    void productionRejectsUnknownProviderNames() {
        assertThatThrownBy(() -> new ProductionSafetyGuard("prod", "wechat", "fake-provider", "aliyun", "s3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不是已知的真实适配器");
    }

    @Test
    void productionRejectsBlankProvider() {
        assertThatThrownBy(() -> new ProductionSafetyGuard("prod", "wechat", "", "aliyun", "s3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未配置");
    }

    @Test
    void demoAndDevAllowMockAdapters() {
        assertThatCode(() -> new ProductionSafetyGuard("demo", "mock", "mock", "mock", "local"))
                .doesNotThrowAnyException();
        assertThatCode(() -> new ProductionSafetyGuard("dev", "mock", "mock", "mock", "local"))
                .doesNotThrowAnyException();
    }

    @Test
    void productionModeFlagIsExposed() {
        assertThat(new ProductionSafetyGuard("prod", "wechat", "kuaidi100", "aliyun", "s3")
                .productionMode()).isTrue();
        assertThat(new ProductionSafetyGuard("demo", "mock", "mock", "mock", "local")
                .productionMode()).isFalse();
    }
}
