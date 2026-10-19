package com.fullmall.supplier.web;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierNonceStoreTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final SupplierNonceStore store = new SupplierNonceStore(jdbcTemplate);
    private static final Instant NOW = Instant.parse("2026-10-19T00:00:00Z");

    @Test
    void firstUseOfNonceIsAccepted() {
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(1);

        assertThat(store.tryRegister("ak-1", "nonce-1", NOW, 300)).isTrue();
        verify(jdbcTemplate).update(anyString(), eq("ak-1"), eq("nonce-1"), any());
    }

    @Test
    void replayedNonceIsRejectedAndSkipsCleanup() {
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(0);

        assertThat(store.tryRegister("ak-1", "nonce-1", NOW, 300)).isFalse();
        // 只有一次 INSERT，没有 DELETE
        verify(jdbcTemplate).update(anyString(), eq("ak-1"), eq("nonce-1"), any());
        verify(jdbcTemplate, never()).update(anyString(), any(java.sql.Timestamp.class));
    }

    @Test
    void acceptedNonceTriggersTtlCleanup() {
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(1);

        store.tryRegister("ak-1", "nonce-2", NOW, 300);

        verify(jdbcTemplate).update(eq("DELETE FROM supplier_nonce WHERE created_at < ?"),
                eq(java.sql.Timestamp.from(NOW.minusSeconds(300))));
    }

    @Test
    void rejectsBlankInputsAndBadTtl() {
        assertThatThrownBy(() -> store.tryRegister(" ", "n", NOW, 300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accessKey");
        assertThatThrownBy(() -> store.tryRegister("ak", " ", NOW, 300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonce");
        assertThatThrownBy(() -> store.tryRegister("ak", "n", NOW, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TTL");
    }
}