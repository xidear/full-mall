package com.fullmall.platform.risk;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskAlertServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final AlertDispatchService dispatch = mock(AlertDispatchService.class);
    private final RiskAlertService service = new RiskAlertService(jdbcTemplate, dispatch);

    @Test
    void skipsWhenSameRuleAndSubjectAlreadyAlertedInWindow() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any(), any()))
                .thenReturn(1L);

        boolean raised = service.raise("EXPORT_SCOPE_DENIED", RiskAlertService.Severity.HIGH,
                "supplier-a", "scope=OWNER", Duration.ofHours(1));

        assertThat(raised).isFalse();
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any(), any());
    }

    @Test
    void insertsWhenNoRecentAlert() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any(), any()))
                .thenReturn(0L);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        boolean raised = service.raise("EXPORT_SCOPE_DENIED", RiskAlertService.Severity.HIGH,
                "supplier-a", "scope=OWNER", Duration.ofHours(1));

        assertThat(raised).isTrue();
        verify(jdbcTemplate).update(anyString(), any(), eq("EXPORT_SCOPE_DENIED"), eq("HIGH"),
                eq("supplier-a"), eq("scope=OWNER"));
    }

    @Test
    void rejectsMissingSubjectAndRule() {
        assertThatThrownBy(() -> service.raise("R", RiskAlertService.Severity.LOW, " ", "d",
                Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("告警对象");
        assertThatThrownBy(() -> service.raise(null, RiskAlertService.Severity.LOW, "s", "d",
                Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("规则编码");
    }

    @Test
    void failsLoudlyWhenInsertAffectsNoRow() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any(), any()))
                .thenReturn(0L);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.raise("R", RiskAlertService.Severity.LOW, "s", "d",
                Duration.ofMinutes(5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("影响行数=0");
    }

    @Test
    void rejectsOutOfRangeLimit() {
        assertThatThrownBy(() -> service.findRecent(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1..500");
        assertThatThrownBy(() -> service.findRecent(501))
                .isInstanceOf(IllegalArgumentException.class);
    }
}