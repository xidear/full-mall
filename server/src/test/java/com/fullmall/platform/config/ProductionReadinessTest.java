package com.fullmall.platform.config;

import com.fullmall.setting.app.SystemSettingService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 生产缺配置**不再阻断启动**，只报告；真实调用时按能力拒绝。 */
class ProductionReadinessTest {

    private static SystemSettingService settings(Map<String, String> values) {
        SystemSettingService mock = mock(SystemSettingService.class);
        when(mock.rawValue(anyString())).thenAnswer(inv ->
                values.getOrDefault(inv.getArgument(0, String.class), ""));
        return mock;
    }

    @Test
    void missingProvidersAreReportedNotBlocking() {
        var checker = new ProductionReadinessChecker(settings(Map.of()), "prod");
        var report = checker.check();

        assertThat(report.production()).isTrue();
        assertThat(report.ready()).isFalse();          // 有缺口
        assertThat(report.items()).extracting(ProductionReadinessChecker.Item::key)
                .containsExactly("payment", "logistics", "sms");
        // 关键：构造与检查都不抛异常——启动不会被阻断
        assertThat(report.items()).allSatisfy(item -> assertThat(item.hint()).isNotBlank());
    }

    @Test
    void configuredRealProvidersAreReady() {
        var checker = new ProductionReadinessChecker(settings(Map.of(
                "payment_provider", "wechat",
                "logistics_provider", "kuaidi100",
                "sms_provider", "aliyun")), "prod");

        assertThat(checker.check().ready()).isTrue();
        assertThat(checker.usableInProduction("payment")).isTrue();
    }

    @Test
    void mockProviderIsNotUsableInProduction() {
        var checker = new ProductionReadinessChecker(settings(Map.of("payment_provider", "mock")), "prod");

        assertThat(checker.usableInProduction("payment")).isFalse();
        assertThat(checker.usableInProduction("logistics")).isFalse();
    }

    @Test
    void demoModeTreatsEverythingAsUsable() {
        var checker = new ProductionReadinessChecker(settings(Map.of()), "demo");

        assertThat(checker.productionMode()).isFalse();
        assertThat(checker.usableInProduction("payment")).isTrue();
        assertThat(checker.usableInProduction("sms")).isTrue();
    }
}
