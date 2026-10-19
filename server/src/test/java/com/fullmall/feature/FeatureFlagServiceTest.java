package com.fullmall.feature;

import com.fullmall.feature.app.FeatureFlagService;
import com.fullmall.feature.domain.FeatureFlag;
import com.fullmall.feature.infra.FeatureFlagRepository;
import com.fullmall.platform.error.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeatureFlagServiceTest {

    private static final Instant START = Instant.parse("2026-10-19T00:00:00Z");

    private final FeatureFlagRepository repository = mock(FeatureFlagRepository.class);
    private final MutableClock clock = new MutableClock(START);

    private FeatureFlagService newService(long ttlSeconds) {
        return new FeatureFlagService(repository, clock, ttlSeconds);
    }

    private static FeatureFlag flag(String code, boolean enabled) {
        return new FeatureFlag(code, enabled, "migration", START, "initial");
    }

    private static Map<String, FeatureFlag> all(boolean enabled) {
        Map<String, FeatureFlag> map = new LinkedHashMap<>();
        map.put(FeatureFlagService.SUPPLIER_ENABLED, flag(FeatureFlagService.SUPPLIER_ENABLED, enabled));
        map.put(FeatureFlagService.POS_SALE_ENABLED, flag(FeatureFlagService.POS_SALE_ENABLED, enabled));
        map.put(FeatureFlagService.POS_PICKUP_ENABLED, flag(FeatureFlagService.POS_PICKUP_ENABLED, enabled));
        return map;
    }

    @Test
    void cachesWithinTtlAndReloadsAfterExpiry() {
        when(repository.findAll()).thenReturn(all(true));
        FeatureFlagService service = newService(30);

        assertThat(service.isEnabled(FeatureFlagService.SUPPLIER_ENABLED)).isTrue();
        clock.advanceSeconds(29);
        assertThat(service.isEnabled(FeatureFlagService.SUPPLIER_ENABLED)).isTrue();
        verify(repository, times(1)).findAll();

        // 过期后必须重新读库，能看到外部变更
        when(repository.findAll()).thenReturn(all(false));
        clock.advanceSeconds(2);
        assertThat(service.isEnabled(FeatureFlagService.SUPPLIER_ENABLED)).isFalse();
        verify(repository, times(2)).findAll();
    }

    @Test
    void unknownCodeFailsInsteadOfDefaulting() {
        when(repository.findAll()).thenReturn(all(true));
        FeatureFlagService service = newService(30);

        assertThatThrownBy(() -> service.isEnabled("not.exists"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未知功能开关");
    }

    @Test
    void updateRequiresReasonAndKnownCode() {
        when(repository.findAll()).thenReturn(all(true));
        FeatureFlagService service = newService(30);

        assertThatThrownBy(() -> service.update("not.exists", false, "关闭", "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("未知功能开关");

        assertThatThrownBy(() -> service.update(FeatureFlagService.SUPPLIER_ENABLED, false, "  ", "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("原因");
    }

    @Test
    void updateInvalidatesCacheAndAuditsActor() {
        when(repository.findAll()).thenReturn(all(true));
        when(repository.update(anyString(), anyBoolean(), anyString(), anyString(), any()))
                .thenReturn(1);

        FeatureFlagService service = newService(30);
        assertThat(service.isEnabled(FeatureFlagService.SUPPLIER_ENABLED)).isTrue();

        // 变更后立即重新读库，返回关闭状态
        when(repository.findAll()).thenReturn(all(false));
        FeatureFlag result = service.update(FeatureFlagService.SUPPLIER_ENABLED, false, "供应商联调暂停", "admin");

        assertThat(result.enabled()).isFalse();
        verify(repository).update(eq(FeatureFlagService.SUPPLIER_ENABLED), eq(false), eq("admin"),
                eq("供应商联调暂停"), eq(START));
        assertThat(service.isEnabled(FeatureFlagService.SUPPLIER_ENABLED)).isFalse();
    }

    @Test
    void updateWithoutMatchingRowFails() {
        when(repository.findAll()).thenReturn(all(true));
        when(repository.update(anyString(), anyBoolean(), anyString(), anyString(), any()))
                .thenReturn(0);

        FeatureFlagService service = newService(30);
        assertThatThrownBy(() -> service.update(FeatureFlagService.SUPPLIER_ENABLED, false, "关闭", "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("影响行数=0");
    }

    @Test
    void rejectsNonPositiveTtl() {
        assertThatThrownBy(() -> newService(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cache-ttl-seconds");
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advanceSeconds(long seconds) {
            this.instant = this.instant.plusSeconds(seconds);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}