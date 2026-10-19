package com.fullmall.supplier.app;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierCredentialStatusTest {

    private static final Instant NOW = Instant.parse("2026-10-19T00:00:00Z");

    @Test
    void hmacOnlyWithoutAllowlistWarnsAboutBoth() {
        List<String> warnings = SupplierCredentialStatus.warnings("HMAC_ONLY", null, null, NOW);

        assertThat(warnings).hasSize(2);
        assertThat(warnings.get(0)).contains("HMAC").contains("Bearer Token");
        assertThat(warnings.get(1)).contains("IP 白名单");
    }

    @Test
    void tokenOnlyWarnsAboutMissingHmac() {
        List<String> warnings = SupplierCredentialStatus.warnings("TOKEN_ONLY", "10.0.0.0/8",
                NOW.plus(Duration.ofDays(30)), NOW);

        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0)).contains("HMAC");
    }

    @Test
    void bothWithFreshTokenAndAllowlistHasNoWarnings() {
        List<String> warnings = SupplierCredentialStatus.warnings("BOTH", "10.0.0.0/8",
                NOW.plus(Duration.ofDays(30)), NOW);

        assertThat(warnings).isEmpty();
    }

    @Test
    void tokenModeWithoutIssuedTokenWarns() {
        List<String> warnings = SupplierCredentialStatus.warnings("BOTH", "10.0.0.0/8", null, NOW);

        assertThat(warnings).anySatisfy(warning -> assertThat(warning).contains("尚未签发"));
    }

    @Test
    void expiredAndExpiringTokensAreDistinguished() {
        assertThat(SupplierCredentialStatus.warnings("BOTH", "10.0.0.0/8",
                NOW.minus(Duration.ofMinutes(1)), NOW))
                .anySatisfy(warning -> assertThat(warning).contains("已过期"));

        assertThat(SupplierCredentialStatus.warnings("BOTH", "10.0.0.0/8",
                NOW.plus(Duration.ofDays(3)), NOW))
                .anySatisfy(warning -> assertThat(warning).contains("7 天内"));
    }
}