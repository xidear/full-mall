package com.fullmall.exchange;

import com.fullmall.exchange.app.ExchangeRateFetchService;
import com.fullmall.exchange.app.ExchangeRateService;
import com.fullmall.platform.secret.IntegrationConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * mock 只在 app.mode=demo 且未配置供应商时兜底；
 * dev/prod 未配置一律不自动拉取（Frankfurter 可用时用真实汇率，不拿固定值糊弄）。
 */
class ExchangeRateFetchModeTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final IntegrationConfigService integrations = mock(IntegrationConfigService.class);
    private final ExchangeRateService rates = mock(ExchangeRateService.class);

    @Test
    void unconfiguredDevDoesNotFetch() {
        when(integrations.enabledProvider("exchange_rate")).thenReturn(null);
        var fetcher = new ExchangeRateFetchService(jdbc, integrations, rates, "CNY", "dev");

        assertThat(fetcher.status().configured()).isFalse();
        assertThat(fetcher.status().provider()).isNull();
        assertThat(fetcher.status().demoMock()).isFalse();
    }

    @Test
    void unconfiguredDemoUsesMock() {
        when(integrations.enabledProvider("exchange_rate")).thenReturn(null);
        var fetcher = new ExchangeRateFetchService(jdbc, integrations, rates, "CNY", "demo");

        assertThat(fetcher.status().configured()).isTrue();
        assertThat(fetcher.status().provider()).isEqualTo("mock");
        assertThat(fetcher.status().demoMock()).isTrue();
    }

    @Test
    void configuredProviderWinsOverDemoDefault() {
        when(integrations.enabledProvider("exchange_rate")).thenReturn("frankfurter");
        var fetcher = new ExchangeRateFetchService(jdbc, integrations, rates, "CNY", "demo");

        assertThat(fetcher.status().provider()).isEqualTo("frankfurter");
        assertThat(fetcher.status().demoMock()).isFalse();
    }

    @Test
    void refreshWithoutConfigWritesNothing() {
        when(integrations.enabledProvider(anyString())).thenReturn(null);
        var fetcher = new ExchangeRateFetchService(jdbc, integrations, rates, "CNY", "prod");

        var status = fetcher.refresh("test");

        assertThat(status.configured()).isFalse();
        org.mockito.Mockito.verifyNoInteractions(rates);
    }
}