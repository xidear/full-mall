package com.fullmall.region;

import com.fullmall.test.TestHttp;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 区划字典与国别地址格式接口的真实 HTTP 测试（ADR-0013）。
 *
 * <p>重点是两条"静默兜底"红线：
 * <ul>
 *   <li>国家清单不能为空——查国家时 country 参数为空，若 SQL 写成 country_code = ?（NULL 比较恒不成立）就会全空；</li>
 *   <li>未配置格式的国家必须 404，不能套中国模板。</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.demo.read-only=false"
        })
@ActiveProfiles("test")
class RegionHttpTest {

    private static final EmbeddedPostgres POSTGRES;

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

    @Value("${local.server.port}")
    int port;

    TestHttp http;

    @BeforeEach
    void setup() {
        http = new TestHttp(port);
    }

    @Test
    void countryListIsNotEmpty() {
        var response = http.exchange("/api/v1/regions/countries?locale=zh-CN", HttpMethod.GET,
                HttpEntity.EMPTY, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("CN").contains("HK");
    }

    @Test
    void singaporeHasNoLocalityOrAdminArea() {
        var response = http.exchange("/api/v1/regions/formats?country=SG", HttpMethod.GET,
                HttpEntity.EMPTY, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        // 新加坡只需地址行 + 6 位邮编：没有省/州，也没有城市
        assertThat(response.getBody()).contains("\"required\":\"AZ\"");
        assertThat(response.getBody()).contains("\"hasLocality\":false");
    }

    @Test
    void unknownCountryFormatIsExplicitlyMissing() {
        var response = http.exchange("/api/v1/regions/formats?country=ZZ", HttpMethod.GET,
                HttpEntity.EMPTY, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).contains("ADDRESS_FORMAT_MISSING");
    }

    @Test
    void chinaReturnsProvincesNotEveryLevel() {
        var response = http.exchange("/api/v1/regions?country=CN&locale=zh-CN", HttpMethod.GET,
                HttpEntity.EMPTY, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        // 34 个省级节点；不能把国家节点与市级一起倒出来
        assertThat(response.getBody()).contains("\"level\":\"ADMIN\"");
        assertThat(response.getBody()).doesNotContain("\"level\":\"COUNTRY\"");
        assertThat(response.getBody()).doesNotContain("\"level\":\"LOCALITY\"");
    }

    @Test
    void provinceChildrenAreCities() {
        var response = http.exchange("/api/v1/regions?country=CN&parent=CN-03&locale=zh-CN",
                HttpMethod.GET, HttpEntity.EMPTY, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"level\":\"LOCALITY\"");
    }
}
