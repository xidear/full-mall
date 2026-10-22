package com.fullmall.licensing;

import com.fullmall.licensing.app.LicenseVerifier;
import com.fullmall.licensing.tool.LicenseIssuer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 签发工具：7 天/永久可签、可验签、域名与到期正确。 */
class LicenseIssuerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    @Test
    void issuedLicenseVerifiesForTrialAndPermanent() throws Exception {
        var keyPair = LicenseIssuer.newKeyPair();

        var trial = LicenseIssuer.issue("试用客户", 7, List.of("shop.example.com"),
                "ENTERPRISE", List.of("ai"), keyPair, Instant.now());
        assertThat(trial.payload().expiresAt()).isNotNull();
        assertThat(trial.payload().expiresAt())
                .isAfter(Instant.now().plus(6, ChronoUnit.DAYS));

        var signed = MAPPER.readValue(trial.licenseJson(), LicenseIssuer.SignedFile.class);
        var verified = LicenseVerifier.verify(
                new LicenseVerifier.Signed(signed.payload(), signed.signature()),
                trial.publicKeyBase64());
        assertThat(verified).isNotNull();
        assertThat(verified.customer()).isEqualTo("试用客户");
        assertThat(LicenseVerifier.domainAllowed(verified.domains(), "shop.example.com")).isTrue();
        assertThat(LicenseVerifier.expired(verified, Instant.now())).isFalse();

        var permanent = LicenseIssuer.issue("永久客户", 0, List.of("a.com"),
                "ENTERPRISE", List.of(), keyPair, Instant.now());
        assertThat(permanent.payload().expiresAt()).isNull();
        assertThat(LicenseVerifier.expired(permanent.payload(), Instant.now().plus(3650, ChronoUnit.DAYS)))
                .isFalse();
    }
}
