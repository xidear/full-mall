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
        // 测试用促销规则互相隔离，避免上一条用例残留的规则影响下一条
        jdbc.update("DELETE FROM promotion WHERE code LIKE 'HTTP-%'");
        UUID marketId = UUID.fromString("99999999-aaaa-bbbb-cccc-000000000001");
        jdbc.update("""
                INSERT INTO market (code, name, currency, country_code, active)
                VALUES ('CN-TEST', '中国测试市场', 'CNY', 'CN', TRUE)
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
                        "countryCode", "CN", "province", "上海", "city", "上海", "district", "静安",
                        "detail", "南京西路 1 号", "postalCode", "200040"), guestHeaders()), String.class);
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
                        "countryCode", "CN", "province", "上海", "city", "上海", "district", "静安",
                        "detail", "南京西路 1 号", "postalCode", "200040"), guestHeaders()), String.class);
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

        // 另一个会员访问同一订单也必须 404（独立证据，不是复用游客分支）
        var otherOtp = http.exchange("/api/v1/member/auth/otp", HttpMethod.POST,
                new HttpEntity<>(Map.of("phone", "13900000999"), guestHeaders()), String.class);
        String otherCode = com.jayway.jsonpath.JsonPath.read(otherOtp.getBody(), "$.debugCode");
        var otherSession = http.exchange("/api/v1/member/auth/verify", HttpMethod.POST,
                new HttpEntity<>(Map.of("phone", "13900000999", "code", otherCode), guestHeaders()),
                String.class);
        String otherToken = com.jayway.jsonpath.JsonPath.read(otherSession.getBody(), "$.token");
        HttpHeaders otherHeaders = new HttpHeaders();
        otherHeaders.set("Authorization", "Bearer " + otherToken);

        var otherDetail = http.exchange("/api/v1/orders/" + createdOrderId, HttpMethod.GET,
                new HttpEntity<>(otherHeaders), String.class);
        assertThat(otherDetail.getStatusCode().value()).isEqualTo(404);
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

        // 登出后 token 立即失效
        var logout = http.exchange("/api/v1/member/auth/logout", HttpMethod.POST,
                new HttpEntity<>(memberHeaders), String.class);
        assertThat(logout.getStatusCode().value()).isEqualTo(200);
        assertThat(logout.getBody()).contains("true");

        var afterLogout = http.exchange("/api/v1/member/auth/me", HttpMethod.GET,
                new HttpEntity<>(memberHeaders), String.class);
        assertThat(afterLogout.getStatusCode().value()).isEqualTo(401);

        // 无效 token 直接 401，不降级为游客
        HttpHeaders bad = new HttpHeaders();
        bad.set("Authorization", "Bearer not-a-real-token");
        var rejected = http.exchange("/api/v1/cart", HttpMethod.GET, new HttpEntity<>(bad), String.class);
        assertThat(rejected.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void checkoutAppliesFullReductionWhenNoOtherPromotion() {
        jdbc.update("DELETE FROM promotion WHERE code = 'HTTP-FULL-20'");
        jdbc.update("""
                INSERT INTO promotion (id, code, type, title, status, starts_at, ends_at, rules, priority)
                VALUES (?, 'HTTP-FULL-20', 'FULL_REDUCTION', '满 20 减 5', 'ACTIVE',
                        now() - interval '1 hour', now() + interval '1 day',
                        '{"thresholdAmount":2000,"value":500}'::jsonb, 10)
                """, UUID.randomUUID());

        seedMemberFor("13800000077");
        http.exchange("/api/v1/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("skuId", SKU.toString(), "quantity", 2), guestHeaders()), String.class);
        var address = http.exchange("/api/v1/addresses", HttpMethod.POST,
                new HttpEntity<>(Map.of("recipient", "满减用户", "phone", "13800000077",
                        "countryCode", "CN", "province", "上海", "city", "上海", "district", "静安",
                        "detail", "南京西路 2 号", "postalCode", "200040"), guestHeaders()), String.class);
        String addressId = com.jayway.jsonpath.JsonPath.read(address.getBody(), "$[0].id");

        var checkout = http.exchange("/api/v1/cart/checkout", HttpMethod.POST,
                new HttpEntity<>(Map.of("addressId", addressId), guestHeaders()), String.class);
        assertThat(checkout.getStatusCode().value()).as(String.valueOf(checkout.getBody())).isEqualTo(200);

        String orderId = com.jayway.jsonpath.JsonPath.read(checkout.getBody(), "$.id");
        assertThat(jdbc.queryForObject(
                "SELECT discount_amount FROM orders WHERE id = ?::uuid", Long.class, orderId))
                .isEqualTo(500L);

        jdbc.update("DELETE FROM promotion WHERE code = 'HTTP-FULL-20'");
    }

    @Test
    void posChannelPromotionIsIgnoredOnline() {
        jdbc.update("DELETE FROM promotion WHERE code = 'HTTP-POS-ONLY'");
        jdbc.update("""
                INSERT INTO promotion (id, code, type, title, status, starts_at, ends_at, rules, priority)
                VALUES (?, 'HTTP-POS-ONLY', 'FULL_REDUCTION', '仅 POS 满减', 'ACTIVE',
                        now() - interval '1 hour', now() + interval '1 day',
                        '{"thresholdAmount":1000,"value":900,"channel":"POS"}'::jsonb, 10)
                """, UUID.randomUUID());

        var response = http.exchange("/api/v1/promotions/preview", HttpMethod.POST,
                new HttpEntity<>(Map.of("items", java.util.List.of(
                        Map.of("skuId", SKU.toString(), "quantity", 2))), guestHeaders()), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).doesNotContain("HTTP-POS-ONLY");

        jdbc.update("DELETE FROM promotion WHERE code = 'HTTP-POS-ONLY'");
    }

    @Test
    void checkoutIgnoresRuleScopedToOtherSkuJustLikePreview() {
        UUID otherSku = UUID.fromString("cccccccc-dddd-eeee-ffff-000000000001");
        jdbc.update("DELETE FROM promotion WHERE code = 'HTTP-SCOPED'");
        jdbc.update("""
                INSERT INTO promotion (id, code, type, title, status, starts_at, ends_at, rules, priority)
                VALUES (?, 'HTTP-SCOPED', 'FULL_REDUCTION', '指定 SKU 满减', 'ACTIVE',
                        now() - interval '1 hour', now() + interval '1 day',
                        ?::jsonb, 10)
                """, UUID.randomUUID(),
                "{\"thresholdAmount\":1000,\"value\":900,\"scopeIds\":[\"" + otherSku + "\"]}");

        var preview = http.exchange("/api/v1/promotions/preview", HttpMethod.POST,
                new HttpEntity<>(Map.of("items", java.util.List.of(
                        Map.of("skuId", SKU.toString(), "quantity", 2))), guestHeaders()), String.class);
        assertThat(preview.getBody()).doesNotContain("HTTP-SCOPED");

        seedMemberFor("13800000088");
        http.exchange("/api/v1/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("skuId", SKU.toString(), "quantity", 2), guestHeaders()), String.class);
        var address = http.exchange("/api/v1/addresses", HttpMethod.POST,
                new HttpEntity<>(Map.of("recipient", "范围用户", "phone", "13800000088",
                        "countryCode", "CN", "province", "上海", "city", "上海", "district", "静安",
                        "detail", "南京西路 3 号", "postalCode", "200040"), guestHeaders()), String.class);
        String addressId = com.jayway.jsonpath.JsonPath.read(address.getBody(), "$[0].id");
        var checkout = http.exchange("/api/v1/cart/checkout", HttpMethod.POST,
                new HttpEntity<>(Map.of("addressId", addressId), guestHeaders()), String.class);
        assertThat(checkout.getStatusCode().value()).isEqualTo(200);

        String orderId = com.jayway.jsonpath.JsonPath.read(checkout.getBody(), "$.id");
        assertThat(jdbc.queryForObject(
                "SELECT discount_amount FROM orders WHERE id = ?::uuid", Long.class, orderId)).isZero();

        jdbc.update("DELETE FROM promotion WHERE code = 'HTTP-SCOPED'");
    }

    @Test
    void categoryScopedRuleAppliesOnlyToMatchingCategory() {
        UUID categoryId = UUID.fromString("99999999-8888-7777-6666-000000000001");
        jdbc.update("""
                INSERT INTO category (id, code, name, active) VALUES (?, 'HTTP-CAT', 'HTTP 类目', TRUE)
                ON CONFLICT (id) DO NOTHING
                """, categoryId);
        jdbc.update("UPDATE product SET category_id = ? WHERE id = 'aaaaaaaa-2222-2222-2222-222222222222'",
                categoryId);
        jdbc.update("DELETE FROM promotion WHERE code = 'HTTP-CAT-RULE'");
        jdbc.update("""
                INSERT INTO promotion (id, code, type, title, status, starts_at, ends_at, rules, priority)
                VALUES (?, 'HTTP-CAT-RULE', 'FULL_REDUCTION', '类目满减', 'ACTIVE',
                        now() - interval '1 hour', now() + interval '1 day', ?::jsonb, 10)
                """, UUID.randomUUID(),
                "{\"thresholdAmount\":1000,\"value\":300,\"scope\":\"CATEGORY\",\"scopeIds\":[\"" + categoryId + "\"]}");

        var matched = http.exchange("/api/v1/promotions/preview", HttpMethod.POST,
                new HttpEntity<>(Map.of("items", java.util.List.of(
                        Map.of("skuId", SKU.toString(), "quantity", 2))), guestHeaders()), String.class);
        assertThat(matched.getBody()).contains("HTTP-CAT-RULE").contains("\"discountAmount\":300");

        // 换个没有类目的商品 → 不命中
        jdbc.update("UPDATE product SET category_id = NULL WHERE id = 'aaaaaaaa-2222-2222-2222-222222222222'");
        var unmatched = http.exchange("/api/v1/promotions/preview", HttpMethod.POST,
                new HttpEntity<>(Map.of("items", java.util.List.of(
                        Map.of("skuId", SKU.toString(), "quantity", 2))), guestHeaders()), String.class);
        assertThat(unmatched.getBody()).doesNotContain("HTTP-CAT-RULE");

        jdbc.update("DELETE FROM promotion WHERE code = 'HTTP-CAT-RULE'");
    }

    @Test
    void brandScopedRuleAppliesOnlyToMatchingBrand() {
        UUID brandId = UUID.fromString("bbbbbbbb-9999-8888-7777-000000000001");
        jdbc.update("UPDATE product SET brand_id = ? WHERE id = 'aaaaaaaa-2222-2222-2222-222222222222'", brandId);
        jdbc.update("""
                INSERT INTO promotion (id, code, type, title, status, starts_at, ends_at, rules, priority)
                VALUES (?, 'HTTP-BRAND-RULE', 'FULL_REDUCTION', '品牌满减', 'ACTIVE',
                        now() - interval '1 hour', now() + interval '1 day', ?::jsonb, 10)
                """, UUID.randomUUID(),
                "{\"thresholdAmount\":1000,\"value\":250,\"scope\":\"BRAND\",\"scopeIds\":[\"" + brandId + "\"]}");

        var matched = http.exchange("/api/v1/promotions/preview", HttpMethod.POST,
                new HttpEntity<>(Map.of("items", java.util.List.of(
                        Map.of("skuId", SKU.toString(), "quantity", 2))), guestHeaders()), String.class);
        assertThat(matched.getBody()).contains("HTTP-BRAND-RULE").contains("\"discountAmount\":250");

        jdbc.update("UPDATE product SET brand_id = NULL WHERE id = 'aaaaaaaa-2222-2222-2222-222222222222'");
        var unmatched = http.exchange("/api/v1/promotions/preview", HttpMethod.POST,
                new HttpEntity<>(Map.of("items", java.util.List.of(
                        Map.of("skuId", SKU.toString(), "quantity", 2))), guestHeaders()), String.class);
        assertThat(unmatched.getBody()).doesNotContain("HTTP-BRAND-RULE");

        jdbc.update("DELETE FROM promotion WHERE code = 'HTTP-BRAND-RULE'");
    }

    @Test
    void posRuleOnlyAppearsWhenPreviewingPosChannel() {
        jdbc.update("""
                INSERT INTO promotion (id, code, type, title, status, starts_at, ends_at, rules, priority)
                VALUES (?, 'HTTP-POS-RULE', 'FULL_REDUCTION', 'POS 满减', 'ACTIVE',
                        now() - interval '1 hour', now() + interval '1 day',
                        '{"thresholdAmount":1000,"value":200,"channel":"POS"}'::jsonb, 10)
                """, UUID.randomUUID());

        var online = http.exchange("/api/v1/promotions/preview", HttpMethod.POST,
                new HttpEntity<>(Map.of("items", java.util.List.of(
                        Map.of("skuId", SKU.toString(), "quantity", 2))), guestHeaders()), String.class);
        assertThat(online.getBody()).doesNotContain("HTTP-POS-RULE");

        var pos = http.exchange("/api/v1/promotions/preview", HttpMethod.POST,
                new HttpEntity<>(Map.of("channel", "POS",
                        "items", java.util.List.of(Map.of("skuId", SKU.toString(), "quantity", 2))),
                        guestHeaders()), String.class);
        assertThat(pos.getBody()).contains("HTTP-POS-RULE").contains("\"discountAmount\":200");
    }

    @Test
    void promotionsListSurvivesRuleOnlyPromotions() {
        // 回归：满减规则没有 skuId，曾导致顾客促销列表整体 404
        jdbc.update("""
                INSERT INTO promotion (id, code, type, title, status, starts_at, ends_at, rules, priority)
                VALUES (?, 'HTTP-RULE-ONLY', 'FULL_REDUCTION', '仅规则促销', 'ACTIVE',
                        now() - interval '1 hour', now() + interval '1 day',
                        '{"thresholdAmount":1000,"value":100}'::jsonb, 10)
                """, UUID.randomUUID());

        var response = http.exchange("/api/v1/promotions", HttpMethod.GET,
                new HttpEntity<>(guestHeaders()), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).doesNotContain("HTTP-RULE-ONLY");
    }

    @Test
    void promotionPreviewAppliesFullReduction() {
        jdbc.update("DELETE FROM promotion WHERE code = 'HTTP-FULL-20'");
        jdbc.update("""
                INSERT INTO promotion (id, code, type, title, status, starts_at, ends_at, rules, priority)
                VALUES (?, 'HTTP-FULL-20', 'FULL_REDUCTION', '满 20 减 5', 'ACTIVE',
                        now() - interval '1 hour', now() + interval '1 day',
                        '{"thresholdAmount":2000,"value":500}'::jsonb, 10)
                """, UUID.randomUUID());

        var response = http.exchange("/api/v1/promotions/preview", HttpMethod.POST,
                new HttpEntity<>(Map.of("items", java.util.List.of(
                        Map.of("skuId", SKU.toString(), "quantity", 2))), guestHeaders()), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("HTTP-FULL-20")
                .contains("\"discountAmount\":500");

        jdbc.update("DELETE FROM promotion WHERE code = 'HTTP-FULL-20'");
    }

    /** 下单要求"真人"：为测试手机号建立会员。 */
    private void seedMemberFor(String phone) {
        jdbc.update("INSERT INTO member (id, member_no, name, phone, tier, points_balance,"
                + " stored_value_balance, currency) VALUES (?, ?, '测试会员', ?, 'STANDARD', 0, 0, 'CNY')"
                + " ON CONFLICT (phone) DO NOTHING", UUID.randomUUID(), "M-" + phone, phone);
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
