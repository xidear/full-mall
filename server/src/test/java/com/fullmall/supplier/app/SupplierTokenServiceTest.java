package com.fullmall.supplier.app;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierTokenServiceTest {

    private static final UUID SUPPLIER = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final SupplierTokenService service = new SupplierTokenService(jdbcTemplate);

    @Test
    void issuedTokenIsUrlSafeAndExpiresInFuture() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any())).thenReturn(1L);
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(1);

        var issued = service.issue(SUPPLIER, Duration.ofHours(24));

        assertThat(issued.token()).matches("[A-Za-z0-9_-]{40,}");
        assertThat(issued.expiresAt()).isAfter(java.time.Instant.now());
        // 只存哈希：写入参数不应等于明文 token
        verify(jdbcTemplate).update(startsWith("UPDATE supplier_credential"),
                org.mockito.ArgumentMatchers.argThat(hash -> !issued.token().equals(hash)), any(), eq(SUPPLIER));
    }

    @Test
    void issueFailsWhenSupplierHasNoCredential() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any())).thenReturn(0L);

        assertThatThrownBy(() -> service.issue(SUPPLIER, Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("没有凭证记录");
    }

    @Test
    void verifyReturnsSupplierWhenTokenMatches() {
        when(jdbcTemplate.query(anyString(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<UUID>>any(),
                anyString())).thenReturn(List.of(SUPPLIER));

        assertThat(service.verify("some-token")).contains(SUPPLIER);
    }

    @Test
    void verifyIsEmptyForBlankOrUnknownToken() {
        when(jdbcTemplate.query(anyString(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<UUID>>any(),
                anyString())).thenReturn(List.of());

        assertThat(service.verify(" ")).isEmpty();
        assertThat(service.verify("unknown")).isEmpty();
    }

    @Test
    void revokeFailsLoudlyWhenNoRowUpdated() {
        when(jdbcTemplate.update(anyString(), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(0);

        assertThatThrownBy(() -> service.revoke(SUPPLIER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("影响行数=0");
    }

    @Test
    void rejectsBadTtl() {
        assertThatThrownBy(() -> service.issue(SUPPLIER, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("有效期");
    }
}