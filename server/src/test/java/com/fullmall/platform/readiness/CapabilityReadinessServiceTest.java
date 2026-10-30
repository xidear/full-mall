package com.fullmall.platform.readiness;

import com.fullmall.platform.secret.IntegrationConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 能力就绪三态：缺失 / 已配置（含来源）/ 两边冲突。
 *
 * <p>冲突必须报 MISMATCH 而不是替用户挑一个——"页面配了 A、环境变量是 B"这种状态
 * 以前被静默按环境变量跑，排查时永远对不上。
 */
class CapabilityReadinessServiceTest {

    private static CapabilityReadinessService.Report service(String storageProvider,
                                                     Map<String, String> storageConfig,
                                                     String paymentIntegration,
                                                     String paymentEnv,
                                                     List<String> aiProviders) {
        IntegrationConfigService integrations = mock(IntegrationConfigService.class);
        when(integrations.enabledProvider(anyString())).thenReturn(null);
        if (storageProvider != null) {
            when(integrations.enabledProvider(eq("storage"))).thenReturn(storageProvider);
            when(integrations.resolveAll(eq("storage"))).thenReturn(storageConfig);
        }
        if (paymentIntegration != null) {
            when(integrations.enabledProvider(eq("payment"))).thenReturn(paymentIntegration);
            when(integrations.resolveAll(eq("payment"))).thenReturn(Map.of());
        }
        when(integrations.resolveAll(eq("export"))).thenReturn(Map.of());

        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(aiProviders);

        return new CapabilityReadinessService(integrations, jdbc, paymentEnv, "", "").check();
    }

    @Test
    void missingCapabilitiesAreReported() {
        var report = service(null, Map.of(), null, "", List.of());

        assertThat(report.ready()).isFalse();
        assertThat(report.items()).anySatisfy(item -> {
            assertThat(item.capability()).isEqualTo("storage");
            assertThat(item.state()).isEqualTo("MISSING");
            assertThat(item.hint()).contains("第三方服务");
        });
        assertThat(report.items()).anySatisfy(item -> {
            assertThat(item.capability()).isEqualTo("ai");
            assertThat(item.state()).isEqualTo("MISSING");
        });
    }

    @Test
    void providerWithoutDirectoryIsIncomplete() {
        var report = service("local", Map.of(), null, "", List.of("ollama"));

        assertThat(report.items()).anySatisfy(item -> {
            assertThat(item.capability()).isEqualTo("storage");
            assertThat(item.state()).isEqualTo("MISSING");
            assertThat(item.missing()).contains("directory");
        });
    }

    @Test
    void configuredStorageWithDirectoryIsReady() {
        var report = service("local", Map.of("directory", "/tmp/x"), null, "", List.of("ollama"));

        assertThat(report.items()).anySatisfy(item -> {
            assertThat(item.capability()).isEqualTo("storage");
            assertThat(item.state()).isEqualTo("CONFIGURED");
            assertThat(item.provider()).isEqualTo("local");
        });
    }

    @Test
    void conflictingPaymentConfigIsMismatch() {
        var report = service(null, Map.of(), "wechat", "mock", List.of("ollama"));

        assertThat(report.items()).anySatisfy(item -> {
            assertThat(item.capability()).isEqualTo("payment");
            assertThat(item.state()).isEqualTo("MISMATCH");
            assertThat(item.hint()).contains("冲突");
        });
    }

    @Test
    void envOnlyPaymentIsFlaggedAsEnvSource() {
        var report = service(null, Map.of(), null, "mock", List.of("ollama"));

        assertThat(report.items()).anySatisfy(item -> {
            assertThat(item.capability()).isEqualTo("payment");
            assertThat(item.state()).isEqualTo("CONFIGURED");
            assertThat(item.hint()).contains("环境变量");
        });
    }
}
