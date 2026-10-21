package com.fullmall.licensing;

import com.fullmall.licensing.app.LicensePayload;
import com.fullmall.licensing.app.LicenseVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 许可证验签、域名匹配与到期判定（纯函数，不依赖数据库）。 */
class LicenseVerifierTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private KeyPair keyPair() throws Exception {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    private LicenseVerifier.Signed sign(LicensePayload payload, KeyPair pair) throws Exception {
        byte[] bytes = MAPPER.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(pair.getPrivate());
        signature.update(bytes);
        return new LicenseVerifier.Signed(
                Base64.getEncoder().encodeToString(bytes),
                Base64.getEncoder().encodeToString(signature.sign()));
    }

    private String publicKey(KeyPair pair) {
        return Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }

    private LicensePayload payload(Instant expiresAt) {
        return new LicensePayload("LIC-1", "演示客户", "ENTERPRISE",
                List.of("ai"), List.of("shop.example.com", "*.demo.example.com"),
                Instant.now().minus(1, ChronoUnit.DAYS), expiresAt, 14);
    }

    @Test
    void validLicenseVerifies() throws Exception {
        KeyPair pair = keyPair();
        var signed = sign(payload(Instant.now().plus(30, ChronoUnit.DAYS)), pair);

        LicensePayload verified = LicenseVerifier.verify(signed, publicKey(pair));

        assertThat(verified).isNotNull();
        assertThat(verified.customer()).isEqualTo("演示客户");
        assertThat(verified.features()).containsExactly("ai");
    }

    @Test
    void tamperedPayloadFailsSignature() throws Exception {
        KeyPair pair = keyPair();
        var signed = sign(payload(Instant.now().plus(30, ChronoUnit.DAYS)), pair);

        String tampered = new String(Base64.getDecoder().decode(signed.payloadBase64()),
                StandardCharsets.UTF_8).replace("演示客户", "盗版客户");
        var forged = new LicenseVerifier.Signed(
                Base64.getEncoder().encodeToString(tampered.getBytes(StandardCharsets.UTF_8)),
                signed.signatureBase64());

        assertThat(LicenseVerifier.verify(forged, publicKey(pair)))
                .as("篡改内容必须验签失败")
                .isNull();
    }

    @Test
    void wrongPublicKeyRejected() throws Exception {
        KeyPair signer = keyPair();
        KeyPair other = keyPair();
        var signed = sign(payload(null), signer);

        assertThat(LicenseVerifier.verify(signed, publicKey(other))).isNull();
    }

    @Test
    void malformedContentThrowsInsteadOfSilentlyPassing() {
        assertThatThrownBy(() -> LicenseVerifier.verify(
                new LicenseVerifier.Signed("not-base64", "also-not"), "bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("许可证无法解析或验签失败");
    }

    @Test
    void expiryHonoursGraceDays() {
        LicensePayload yesterday = payload(Instant.now().minus(1, ChronoUnit.DAYS));
        LicensePayload longAgo = payload(Instant.now().minus(30, ChronoUnit.DAYS));
        LicensePayload forever = payload(null);

        assertThat(LicenseVerifier.expired(yesterday, Instant.now()))
                .as("过期 1 天但在 14 天宽限内，不算过期").isFalse();
        assertThat(LicenseVerifier.expired(longAgo, Instant.now())).isTrue();
        assertThat(LicenseVerifier.expired(forever, Instant.now()))
                .as("expiresAt=null 表示永久").isFalse();
    }

    @Test
    void domainMatchingRules() {
        List<String> domains = List.of("shop.example.com", "*.demo.example.com");

        assertThat(LicenseVerifier.domainAllowed(domains, "shop.example.com")).isTrue();
        assertThat(LicenseVerifier.domainAllowed(domains, "SHOP.EXAMPLE.COM:8443")).isTrue();
        assertThat(LicenseVerifier.domainAllowed(domains, "a.demo.example.com")).isTrue();
        assertThat(LicenseVerifier.domainAllowed(domains, "a.b.demo.example.com"))
                .as("二级通配不匹配").isFalse();
        assertThat(LicenseVerifier.domainAllowed(domains, "evil.com")).isFalse();
        assertThat(LicenseVerifier.domainAllowed(domains, "localhost"))
                .as("本机永远放行").isTrue();
    }
}
