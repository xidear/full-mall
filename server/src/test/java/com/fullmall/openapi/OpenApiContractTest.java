package com.fullmall.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.fullmall.test.TestHttp;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAPI 契约测试：springdoc 必须能生成文档，且关键接口都在契约里。
 *
 * <p>这是 CI 的 “OpenAPI diff” 基线：任何接口被误删/改路径都会在这里失败，
 * 而不是等到前端或供应商对接时才发现。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:openapi;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "app.payment.provider=mock",
                "app.demo.read-only=false"
        })
@ActiveProfiles("test")
class OpenApiContractTest {

    private static final List<String> REQUIRED_PATHS = List.of(
            "/api/v1/products",
            "/api/v1/checkout",
            "/admin/v1/auth/login",
            "/admin/v1/feature-flags",
            "/admin/v1/assets",
            "/store/v1/pos/sales",
            "/store/v1/pickups/{pickupCode}/verify",
            "/open/v1",
            "/open/v1/orders/{fulfillmentId}",
            "/open/v1/inventory/feed");

    @org.springframework.boot.test.context.TestConfiguration
    static class PermitAllSecurity {
        @org.springframework.context.annotation.Bean
        org.springframework.security.web.SecurityFilterChain testChain(
                org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
            return http.csrf(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @org.springframework.beans.factory.annotation.Value("${local.server.port}")
    int port;

    TestHttp rest;

    @org.junit.jupiter.api.BeforeEach
    void initHttp() {
        rest = new TestHttp(port);
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void apiDocsAreGeneratedAndContainCriticalPaths() throws Exception {
        ResponseEntity<String> response = rest.getForEntity("/v3/api-docs", String.class);
        assertThat(response.getStatusCode().value())
                .as("springdoc 必须能生成 OpenAPI 文档")
                .isEqualTo(200);

        JsonNode document = objectMapper.readTree(response.getBody());
        assertThat(document.path("openapi").asText()).startsWith("3.");

        List<String> paths = new ArrayList<>();
        document.path("paths").fieldNames().forEachRemaining(paths::add);

        for (String required : REQUIRED_PATHS) {
            boolean present = paths.stream().anyMatch(path -> path.equals(required) || path.startsWith(required));
            assertThat(present)
                    .as("契约缺少关键接口: %s（当前共有 %d 个路径）", required, paths.size())
                    .isTrue();
        }
    }
}