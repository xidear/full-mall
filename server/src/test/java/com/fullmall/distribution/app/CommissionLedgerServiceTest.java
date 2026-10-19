package com.fullmall.distribution.app;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommissionLedgerServiceTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CommissionLedgerService service = new CommissionLedgerService(jdbc);

    @Test
    void releaseSkipsWhenNothingDue() {
        when(jdbc.query(anyString(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<CommissionLedgerService.Commission>>any(),
                any())).thenReturn(List.of());

        assertThat(service.releaseDue(java.time.Instant.now())).isZero();
        verify(jdbc, never()).update(anyString(), any(), any());
    }

    @Test
    void reverseWithoutReasonIsRejected() {
        assertThatThrownBy(() -> service.reverse(UUID.randomUUID(), "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("原因");
        verify(jdbc, never()).update(anyString(), any(), any());
    }

    @Test
    void withdrawnCommissionCannotBeReversedSilently() {
        UUID id = UUID.randomUUID();
        when(jdbc.query(anyString(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<CommissionLedgerService.Commission>>any(),
                any())).thenReturn(List.of(new CommissionLedgerService.Commission(
                id, UUID.randomUUID(), UUID.randomUUID(), 500, "CNY", "WITHDRAWN", null)));

        assertThatThrownBy(() -> service.reverse(id, "退款"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("已提现");
    }

    @Test
    void availableBalanceExcludesFrozen() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any())).thenReturn(1200L);

        assertThat(service.availableBalance(UUID.randomUUID())).isEqualTo(1200L);
    }
}
