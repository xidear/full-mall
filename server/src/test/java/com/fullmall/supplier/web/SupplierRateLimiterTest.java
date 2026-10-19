package com.fullmall.supplier.web;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierRateLimiterTest {

    private static final Instant NOW = Instant.parse("2026-10-19T00:00:30Z");
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final SupplierRateLimiter limiter = new SupplierRateLimiter(jdbcTemplate);

    @Test
    void allowsWhenUnderLimit() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(3);

        var decision = limiter.check("ak-1", 10, WINDOW, NOW);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.currentCount()).isEqualTo(3);
        assertThat(decision.retryAfterSeconds()).isZero();
    }

    @Test
    void rejectsWithRetryAfterWhenOverLimit() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(11);

        var decision = limiter.check("ak-1", 10, WINDOW, NOW);

        assertThat(decision.allowed()).isFalse();
        // 窗口 00:00:00–00:01:00，当前 00:00:30，应剩余 30 秒
        assertThat(decision.retryAfterSeconds()).isEqualTo(30);
    }

    @Test
    void firstRequestOfWindowTriggersCleanupOnlyOnce() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(1);

        limiter.check("ak-1", 10, WINDOW, NOW);

        verify(jdbcTemplate).update(anyString(), any(java.sql.Timestamp.class));
    }

    @Test
    void nonFirstRequestSkipsCleanup() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(5);

        limiter.check("ak-1", 10, WINDOW, NOW);

        verify(jdbcTemplate, never()).update(anyString(), any(java.sql.Timestamp.class));
    }

    @Test
    void zeroLimitMeansExplicitlyDisabled() {
        var decision = limiter.check("ak-1", 0, WINDOW, NOW);

        assertThat(decision.allowed()).isTrue();
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Integer.class), any(), any());
    }

    @Test
    void rejectsBadInputs() {
        assertThatThrownBy(() -> limiter.check(" ", 10, WINDOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accessKey");
        assertThatThrownBy(() -> limiter.check("ak", 10, Duration.ZERO, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("窗口");
    }
}