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
                "app.demo.read-only=false",
                "app.member.otp.return-code=true"
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
        UUID marketId = UUID.fromString("99999999-aaaa-bbbb-cccc-000000000001");
        jdbc.update("""
                INSERT INTO market (code, name, currency, default_locale, active)
                VALUES ('CN-TEST', '中国测试市场', 'CNY', 'zh-CN', TRUE)
                ON CONFLICT (code) DO NOTHING
                """);
        UUID warehouseId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        jdbc.update("""
                INSERT INTO warehouse (id, market_code, name, type, active)
                VALUES (?, 'CN-TEST', 'HTTP 测试仓', 'SELF', TRUE)
                ON CONFLICT (id) DO NOTHING
                """, warehouseId);

        UUID productId = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, 'HTTP 测试商品', 'ACTIVE') ON CONFLICT (id) DO NOTHING", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, 'SKU-HTTP', 1000, 'CNY') ON CONFLICT (id) DO NOTHING
                """, SKU, productId);
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no,
                                             quantity, reserved_quantity, status)
                VALUES ('dddddddd-1111-2222-3333-444444444444', ?, 'WAREHOUSE', ?, 'HTTP-BATCH', 50, 0, 'AVAILABLE')
                ON CONFLICT (id) DO NOTHING
                """, SKU, warehouseId);
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
    void guestCanCheckoutFromCartAndCartIsCleared() {
        http.exchange("/api/v1/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("skuId", SKU.toString(), "quantity", 2), guestHeaders()), String.class);

        var addressResponse = http.exchange("/api/v1/addresses", HttpMethod.POST,
                new HttpEntity<>(Map.of("recipient", "张三", "phone", "13800000011",
                        "province", "上海", "city", "上海", "district", "静安",
                        "detail", "南京西路 1 号"), guestHeaders()), String.class);
        String addressId = com.jayway.jsonpath.JsonPath.read(addressResponse.getBody(), "$[0].id");

        // GOLD 会员（95 折）用于验证 MEM-002
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, 'M-GOLD-1', '张三', '13800000011', 'GOLD', 0, 0, 'CNY')
                ON CONFLICT (phone) DO NOTHING
                """, UUID.randomUUID());
        jdbc.update("""
                INSERT INTO referral_binding (id, customer_phone, promoter_staff_id, mode, active)
                VALUES (?, '13800000011', ?, 'ORDER', TRUE)
                """, UUID.randomUUID(), UUID.randomUUID());

        var checkout = http.exchange("/api/v1/cart/checkout", HttpMethod.POST,
                new HttpEntity<>(Map.of("addressId", addressId, "fulfillmentMode", "SHIP"), guestHeaders()),
                String.class);

        assertThat(checkout.getStatusCode().value()).isEqualTo(200);
        assertThat(checkout.getBody()).contains("orderNo");
        // MEM-002：GOLD 95 折，2000 小计 → 会员折扣 100
        String createdOrderId = com.jayway.jsonpath.JsonPath.read(checkout.getBody(), "$.id");
        assertThat(jdbc.queryForObject(
                "SELECT discount_amount FROM orders WHERE id = ?::uuid", Long.class, createdOrderId))
                .isEqualTo(100L);
        // 订单已按手机号绑定会员
        assertThat(jdbc.queryForObject(
                "SELECT member_id FROM orders WHERE id = ?::uuid", java.util.UUID.class, createdOrderId))
                .isNotNull();

        // 订单详情越权保护：游客访问已绑定会员的订单 → 404
        var anonymousDetail = http.exchange("/api/v1/orders/" + createdOrderId, HttpMethod.GET,
                new HttpEntity<>(guestHeaders()), String.class);
        assertThat(anonymousDetail.getStatusCode().value()).isEqualTo(404);
        // 下单后购物车清空
        var cart = http.exchange("/api/v1/cart", HttpMethod.GET,
                new HttpEntity<>(guestHeaders()), String.class);
        assertThat(cart.getBody()).isEqualTo("[]");

        // 支付 → 佣金应自动生成并处于冻结状态（绑定在下单前建立，下单时会挂上级）
        String orderId = com.jayway.jsonpath.JsonPath.read(checkout.getBody(), "$.id");
        var pay = http.exchange("/api/v1/payments/mock/" + orderId + "/pay", HttpMethod.POST,
                new HttpEntity<>(guestHeaders()), String.class);
        assertThat(pay.getStatusCode().value()).isEqualTo(200);

        Integer commissionRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM distribution_commission WHERE order_id = ?::uuid", Integer.class, orderId);
        assertThat(commissionRows).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM distribution_commission WHERE order_id = ?::uuid", String.class, orderId))
                .isEqualTo("FROZEN");
    }

    @Test
    void memberTokenScopesCartToMemberIdentity() {
        // 登录拿 token
        var otp = http.exchange("/api/v1/member/auth/otp", HttpMethod.POST,
                new HttpEntity<>(Map.of("phone", "13900000123"), guestHeaders()), String.class);
        String code = com.jayway.jsonpath.JsonPath.read(otp.getBody(), "$.debugCode");
        var session = http.exchange("/api/v1/member/auth/verify", HttpMethod.POST,
                new HttpEntity<>(Map.of("phone", "13900000123", "code", code), guestHeaders()), String.class);
        String token = com.jayway.jsonpath.JsonPath.read(session.getBody(), "$.token");

        HttpHeaders memberHeaders = new HttpHeaders();
        memberHeaders.set("Authorization", "Bearer " + token);
        memberHeaders.set("Content-Type", "application/json");

        // 会员加购
        var add = http.exchange("/api/v1/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("skuId", SKU.toString(), "quantity", 3), memberHeaders), String.class);
        assertThat(add.getStatusCode().value()).as(String.valueOf(add.getBody())).isEqualTo(200);

        // 游客车看不到会员的商品
        var guestCart = http.exchange("/api/v1/cart", HttpMethod.GET,
                new HttpEntity<>(guestHeaders()), String.class);
        assertThat(guestCart.getBody()).isEqualTo("[]");

        // 会员车能看到
        var memberCart = http.exchange("/api/v1/cart", HttpMethod.GET,
                new HttpEntity<>(memberHeaders), String.class);
        assertThat(memberCart.getBody()).contains("\"quantity\":3");

        // MEM-002：支付后按 GOLD 2 倍积分累积（实付 1900 → 基数 19 → 38 分）
        Integer points = jdbc.queryForObject(
                "SELECT points_balance FROM member WHERE phone = '13800000011'", Integer.class);
        assertThat(points).isEqualTo(38);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM points_ledger WHERE business_key LIKE 'order:%:points'", Integer.class))
                .isEqualTo(1);

        // 我的订单：只能看到自己的
        var myOrders = http.exchange("/api/v1/orders/mine", HttpMethod.GET,
                new HttpEntity<>(memberHeaders), String.class);
        assertThat(myOrders.getStatusCode().value()).isEqualTo(200);
        // 该会员当前没有订单，返回空数组；越权保护由下一条断言（游客被拒）覆盖
        assertThat(myOrders.getBody()).isEqualTo("[]");

        // 未登录不能看"我的订单"（明确拒绝，而不是返回空列表）
        var guestOrders = http.exchange("/api/v1/orders/mine", HttpMethod.GET,
                new HttpEntity<>(guestHeaders()), String.class);
        assertThat(guestOrders.getStatusCode().value()).isEqualTo(400);
        assertThat(guestOrders.getBody()).contains("需要会员登录");

        // 会员可取自己的资料（掩码）
        var me = http.exchange("/api/v1/member/auth/me", HttpMethod.GET,
                new HttpEntity<>(memberHeaders), String.class);
        assertThat(me.getStatusCode().value()).isEqualTo(200);
        assertThat(me.getBody()).contains("139****0123").doesNotContain("13900000123");

        // 无效 token 直接 401，不降级为游客
        HttpHeaders bad = new HttpHeaders();
        bad.set("Authorization", "Bearer not-a-real-token");
        var rejected = http.exchange("/api/v1/cart", HttpMethod.GET, new HttpEntity<>(bad), String.class);
        assertThat(rejected.getStatusCode().value()).isEqualTo(401);
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
