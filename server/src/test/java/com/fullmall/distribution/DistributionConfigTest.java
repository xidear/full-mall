package com.fullmall.distribution;

import com.fullmall.distribution.app.DistributionConfig;
import com.fullmall.setting.app.SystemSettingService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 分销比例/等级/冻结/提现全部来自设置；非法配置必须报错而不是静默取默认值。 */
class DistributionConfigTest {

    private static SystemSettingService settings(java.util.Map<String, String> values) {
        SystemSettingService mock = mock(SystemSettingService.class);
        when(mock.rawValue(anyString())).thenAnswer(inv ->
                values.getOrDefault(inv.getArgument(0, String.class), ""));
        return mock;
    }

    @Test
    void defaultsArePercentageBasedTwoLevels() {
        var config = new DistributionConfig(settings(java.util.Map.of())).load();

        assertThat(config.level1().mode()).isEqualTo(DistributionConfig.RateMode.GROSS);
        assertThat(config.level1().value()).isEqualTo(5);
        assertThat(config.level2Enabled()).isTrue();
        assertThat(config.freezePolicy()).isEqualTo("AFTER_SALE_CLOSED");
        assertThat(config.minWithdrawal()).isEqualTo(10_000);
        assertThat(config.payoutMethods()).containsExactly("ALIPAY", "BANK");
    }

    @Test
    void readsConfiguredRatesAndModes() {
        var values = java.util.Map.of(
                "distribution.level1-mode", "FIXED",
                "distribution.level1-value", "800",
                "distribution.level2-mode", "GROSS",
                "distribution.level2-value", "2",
                "distribution.min-withdrawal", "5000",
                "distribution.payout-methods", "ALIPAY,WECHAT");
        var config = new DistributionConfig(settings(values)).load();

        assertThat(config.level1().mode()).isEqualTo(DistributionConfig.RateMode.FIXED);
        assertThat(DistributionConfig.commission(config, 1, 20_000)).isEqualTo(800);
        assertThat(DistributionConfig.commission(config, 2, 20_000)).isEqualTo(400);
        assertThat(config.minWithdrawal()).isEqualTo(5_000);
        assertThat(config.payoutMethods()).containsExactly("ALIPAY", "WECHAT");
    }

    @Test
    void percentageOutOfRangeIsRejected() {
        var config = new DistributionConfig(settings(
                java.util.Map.of("distribution.level1-value", "150")));

        assertThatThrownBy(config::load)
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("1..100");
    }

    @Test
    void invalidModeAndFreezePolicyAreRejected() {
        assertThatThrownBy(() -> new DistributionConfig(settings(
                java.util.Map.of("distribution.level1-mode", "CHAOS"))).load())
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("GROSS 或 FIXED");

        assertThatThrownBy(() -> new DistributionConfig(settings(
                java.util.Map.of("distribution.freeze-policy", "FOREVER"))).load())
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("冻结策略");
    }

    @Test
    void level2CanBeDisabled() {
        var config = new DistributionConfig(settings(
                java.util.Map.of("distribution.level2-enabled", "false"))).load();

        assertThat(config.level2Enabled()).isFalse();
        assertThat(config.level2().value()).isZero();
    }
}
