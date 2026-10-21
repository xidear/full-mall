package com.fullmall.licensing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.licensing.app.LicensePayload;
import com.fullmall.licensing.app.LicenseService;
import com.fullmall.licensing.config.LicenseProperties;
import com.fullmall.licensing.web.LicenseReadOnlyFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 到期只读：GET 放行、写操作 402、登录与许可上传必须仍然可用。 */
class LicenseReadOnlyFilterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    @RestController
    static class DummyController {
        @GetMapping("/api/v1/products")
        String list() {
            return "ok";
        }

        @PostMapping(value = "/api/v1/checkout", produces = MediaType.APPLICATION_JSON_VALUE)
        String checkout() {
            return "ok";
        }

        @PostMapping("/admin/v1/auth/login")
        String login() {
            return "{\"token\":\"t\"}";
        }

        @PostMapping("/api/v1/payments/mock/x/pay")
        String payCallback() {
            return "ok";
        }
    }

    private Path licenseFile;
    private LicenseService service;

    private void prepareLicense(Instant expiresAt) throws Exception {
        KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        LicensePayload payload = new LicensePayload("LIC-1", "演示客户", "ENTERPRISE",
                List.of("ai"), List.of("localhost"), Instant.now().minus(30, ChronoUnit.DAYS),
                expiresAt, 0);
        byte[] bytes = MAPPER.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(pair.getPrivate());
        signature.update(bytes);
        String content = MAPPER.writeValueAsString(new LicenseFile(
                Base64.getEncoder().encodeToString(bytes),
                Base64.getEncoder().encodeToString(signature.sign())));
        licenseFile = Files.createTempFile("license", ".json");
        Files.writeString(licenseFile, content);

        LicenseProperties properties = new LicenseProperties();
        properties.setEnabled(true);
        properties.setFile(licenseFile.toString());
        properties.setPublicKey(Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
        properties.setDomainCheck(false);
        service = new LicenseService(properties,
                new StaticListableBeanFactory().getBeanProvider(org.springframework.jdbc.core.JdbcTemplate.class));
    }

    record LicenseFile(String payload, String signature) {
    }

    @BeforeEach
    void setUp() throws Exception {
        prepareLicense(Instant.now().minus(2, ChronoUnit.DAYS));   // 已过期、宽限 0 天
    }

    @AfterEach
    void tearDown() throws Exception {
        if (licenseFile != null) {
            Files.deleteIfExists(licenseFile);
        }
    }

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(new DummyController())
                .addFilters(new LicenseReadOnlyFilter(service))
                .build();
    }

    @Test
    void expiredLicenseAllowsReadsAndBlocksWrites() throws Exception {
        mvc().perform(get("/api/v1/products")).andExpect(status().isOk());
        mvc().perform(post("/api/v1/checkout"))
                .andExpect(status().is(402))
                .andExpect(jsonPath("$.code").value("LICENSE_EXPIRED"));
    }

    @Test
    void loginAndPaymentCallbacksStayAvailableWhenExpired() throws Exception {
        mvc().perform(post("/admin/v1/auth/login")).andExpect(status().isOk());
        mvc().perform(post("/api/v1/payments/mock/x/pay"))
                .andExpect(status().isOk());
    }

    @Test
    void activeLicenseAllowsWrites() throws Exception {
        prepareLicense(Instant.now().plus(30, ChronoUnit.DAYS));

        mvc().perform(post("/api/v1/checkout")).andExpect(status().isOk());
    }
}
