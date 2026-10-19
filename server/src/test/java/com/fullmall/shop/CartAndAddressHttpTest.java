package com.fullmall.shop;

import com.fullmall.test.TestHttp;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 真实 HTTP：游客加购 → 建地址 → 查列表。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.demo.read-only=false"
        })
@ActiveProfiles("test")
class CartAndAddressHttpTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final UUID SKU = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

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

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setup() {
        http = new TestHttp(port);
        jdbc.update("DELETE FROM cart_item");
        jdbc.update("DELETE FROM cart");
        jdbc.update("DELETE FROM member_address");
        UUID productId = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, 'HTTP 测试商品', 'ACTIVE') ON CONFLICT (id) DO NOTHING", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, 'SKU-HTTP', 1000, 'CNY') ON CONFLICT (id) DO NOTHING
                """, SKU, productId);
    }

    private HttpHeaders guestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Cart-Token", "http-token-1");
        headers.set("Content-Type", "application/json");
        return headers;
    }

    @Test
    void guestCanAddToCartAndCreateAddress() {
        var cartResponse = http.exchange("/api/v1/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("skuId", SKU.toString(), "quantity", 2), guestHeaders()),
                String.class);
        assertThat(cartResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(cartResponse.getBody()).contains("\"quantity\":2");

        var addressResponse = http.exchange("/api/v1/addresses", HttpMethod.POST,
                new HttpEntity<>(Map.of("recipient", "张三", "phone", "13800000009",
                        "province", "上海", "city", "上海", "district", "静安",
                        "detail", "南京西路 1 号"), guestHeaders()), String.class);
        assertThat(addressResponse.getStatusCode().value()).isEqualTo(201);
        assertThat(addressResponse.getBody()).contains("138****0009");

        var listResponse = http.exchange("/api/v1/addresses", HttpMethod.GET,
                new HttpEntity<>(guestHeaders()), String.class);
        assertThat(listResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(listResponse.getBody()).contains("\"isDefault\":true");
    }

    @Test
    void checkoutWithEmptyCartIsRejected() {
        var response = http.exchange("/api/v1/cart/checkout", HttpMethod.POST,
                new HttpEntity<>(Map.of(), guestHeaders()), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("购物车为空");
    }

    @Test
    void checkoutWithoutAddressIsRejected() {
        http.exchange("/api/v1/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("skuId", SKU.toString(), "quantity", 1), guestHeaders()), String.class);

        var response = http.exchange("/api/v1/cart/checkout", HttpMethod.POST,
                new HttpEntity<>(Map.of(), guestHeaders()), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("必须选择收货地址");
    }

    @Test
    void missingGuestTokenIsRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        var response = http.exchange("/api/v1/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("skuId", SKU.toString(), "quantity", 1), headers), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("X-Cart-Token");
    }
}
