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
 * 供应商限流端到端（SUP-033）：额度调到 2 后，第三次请求必须 429 且带 Retry-After。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.payment.provider=mock",
                "app.demo.read-only=false",
                "app.supplier.rate-limit.max-requests=2",
                "app.supplier.rate-limit.window-seconds=3600"
        })
@ActiveProfiles("test")
class SupplierRateLimitTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final UUID SUPPLIER_ID = UUID.fromString("44444444-4444-4444-4444-44444444444a");
    private static final String ACCESS_KEY = "rl-access-key";
    private static final String SECRET = "rl-secret";

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

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM supplier_rate_window");
        jdbc.update("DELETE FROM supplier_nonce");
        jdbc.update("DELETE FROM supplier_credential");
        jdbc.update("DELETE FROM supplier WHERE id = ?", SUPPLIER_ID);
        jdbc.update("""
                INSERT INTO supplier (id, code, name, active) VALUES (?, 'RL-SUP', '限流测试供应商', TRUE)
                """, SUPPLIER_ID);
        jdbc.update("""
                INSERT INTO supplier_credential (id, supplier_id, access_key, secret_encrypted, active, ip_allowlist)
                VALUES (?, ?, ?, ?, TRUE, NULL)
                """, UUID.randomUUID(), SUPPLIER_ID, ACCESS_KEY, cipher.encrypt(SECRET));
    }

    private ResponseEntity<String> ping() throws Exception {
        String nonce = "rl-" + UUID.randomUUID();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String canonical = "GET\n/open/v1/ping\n" + timestamp + "\n" + nonce + "\n" + sha256Hex(new byte[0]);
        HttpHeaders headers = new HttpHeaders();
        headers.set(SupplierHmacFilter.ACCESS_KEY_HEADER, ACCESS_KEY);
        headers.set(SupplierHmacFilter.TIMESTAMP_HEADER, timestamp);
        headers.set(SupplierHmacFilter.NONCE_HEADER, nonce);
        headers.set(SupplierHmacFilter.SIGNATURE_HEADER, hmacHex(SECRET, canonical));
        return rest.exchange("/open/v1/ping", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    @Test
    void thirdRequestInWindowIsRateLimited() throws Exception {
        assertThat(ping().getStatusCode().value()).isEqualTo(200);
        assertThat(ping().getStatusCode().value()).isEqualTo(200);

        ResponseEntity<String> limited = ping();
        assertThat(limited.getStatusCode().value()).isEqualTo(429);
        assertThat(limited.getBody()).contains("SUPPLIER_RATE_LIMITED");
        assertThat(limited.getHeaders().getFirst("Retry-After")).isNotBlank();
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