package com.fullmall.supplier;

import com.fullmall.supplier.app.SupplierSecretCipher;
import com.fullmall.supplier.web.SupplierHmacFilter;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * /open/v1 端到端安全测试（SUP-032 / SUP-030）。
 *
 * <p>覆盖：正常签名放行、同 nonce 重放拒绝、签名错误拒绝、IP 白名单外拒绝。
 * 这是前几轮缺失的“真实请求验证”，避免安全机制只在单测里看起来正确。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.payment.provider=mock",
                "app.demo.read-only=false"
        })
@ActiveProfiles("test")
class SupplierOpenSecurityTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final UUID SUPPLIER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String ACCESS_KEY = "it-access-key";
    private static final String SECRET = "it-secret-value";

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
        } catch (Exception error) {
            throw new IllegalStateException("无法启动嵌入式 PostgreSQL", error);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class PermitAll {
        @org.springframework.context.annotation.Bean
        org.springframework.security.web.SecurityFilterChain chain(
                org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
            return http.csrf(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SupplierSecretCipher cipher;

    @Autowired
    com.fullmall.supplier.app.SupplierTokenService tokenService;

    @BeforeEach
    void seedCredential() {
        jdbc.update("DELETE FROM supplier_nonce");
        jdbc.update("DELETE FROM supplier_credential");
        jdbc.update("DELETE FROM supplier WHERE id = ?", SUPPLIER_ID);
        jdbc.update("""
                INSERT INTO supplier (id, code, name, active) VALUES (?, 'IT-SUP', '集成测试供应商', TRUE)
                ON CONFLICT (id) DO NOTHING
                """, SUPPLIER_ID);
        jdbc.update("""
                INSERT INTO supplier_credential (id, supplier_id, access_key, secret_encrypted, active, ip_allowlist)
                VALUES (?, ?, ?, ?, TRUE, NULL)
                """, UUID.randomUUID(), SUPPLIER_ID, ACCESS_KEY, cipher.encrypt(SECRET));
    }

    private HttpHeaders signedHeaders(String nonce, String secret) throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String canonical = "GET\n/open/v1/ping\n" + timestamp + "\n" + nonce + "\n" + sha256Hex(new byte[0]);
        HttpHeaders headers = new HttpHeaders();
        headers.set(SupplierHmacFilter.ACCESS_KEY_HEADER, ACCESS_KEY);
        headers.set(SupplierHmacFilter.TIMESTAMP_HEADER, timestamp);
        headers.set(SupplierHmacFilter.NONCE_HEADER, nonce);
        headers.set(SupplierHmacFilter.SIGNATURE_HEADER, hmacHex(secret, canonical));
        return headers;
    }

    private ResponseEntity<String> ping(String nonce, String secret) throws Exception {
        return rest.exchange("/open/v1/ping", HttpMethod.GET,
                new HttpEntity<>(signedHeaders(nonce, secret)), String.class);
    }

    @Test
    void validSignatureIsAccepted() throws Exception {
        ResponseEntity<String> response = ping("nonce-ok-" + UUID.randomUUID(), SECRET);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains(SUPPLIER_ID.toString());
    }

    @Test
    void replayedNonceIsRejected() throws Exception {
        String nonce = "nonce-replay-" + UUID.randomUUID();
        assertThat(ping(nonce, SECRET).getStatusCode().value()).isEqualTo(200);

        ResponseEntity<String> replay = ping(nonce, SECRET);
        assertThat(replay.getStatusCode().value()).isEqualTo(409);
        assertThat(replay.getBody()).contains("SUPPLIER_NONCE_REPLAYED");
    }

    @Test
    void wrongSignatureIsRejected() throws Exception {
        ResponseEntity<String> response = ping("nonce-bad-" + UUID.randomUUID(), "wrong-secret");
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).contains("SUPPLIER_SIGNATURE_INVALID");
    }

    @Test
    void ipOutsideAllowlistIsRejected() throws Exception {
        jdbc.update("UPDATE supplier_credential SET ip_allowlist = '10.0.0.0/8' WHERE access_key = ?", ACCESS_KEY);

        ResponseEntity<String> response = ping("nonce-ip-" + UUID.randomUUID(), SECRET);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).contains("SUPPLIER_IP_NOT_ALLOWED");
    }

    @Test
    void bearerTokenIsAccepted() throws Exception {
        jdbc.update("UPDATE supplier_credential SET auth_mode = 'BOTH' WHERE access_key = ?", ACCESS_KEY);
        var issued = tokenService.issue(SUPPLIER_ID, java.time.Duration.ofHours(1));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + issued.token());
        ResponseEntity<String> response = rest.exchange("/open/v1/ping", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains(SUPPLIER_ID.toString());
    }

    @Test
    void revokedTokenIsRejected() throws Exception {
        jdbc.update("UPDATE supplier_credential SET auth_mode = 'BOTH' WHERE access_key = ?", ACCESS_KEY);
        var issued = tokenService.issue(SUPPLIER_ID, java.time.Duration.ofHours(1));
        tokenService.revoke(SUPPLIER_ID);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + issued.token());
        ResponseEntity<String> response = rest.exchange("/open/v1/ping", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).contains("SUPPLIER_TOKEN_INVALID");
    }

    private static String sha256Hex(byte[] body) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
    }

    private static String hmacHex(String secret, String canonical) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    }
}