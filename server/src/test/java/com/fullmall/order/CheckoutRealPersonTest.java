package com.fullmall.order;

import com.fullmall.order.api.CheckoutRequest;
import com.fullmall.order.app.OrderService;
import com.fullmall.order.domain.FulfillmentMode;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 下单必须绑定真人：未注册手机号需验证码，验证后自动建会员并绑定订单。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.member.otp.return-code=true"
        })
@ActiveProfiles("test")
class CheckoutRealPersonTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final UUID PRODUCT = UUID.fromString("aeaeaeae-0000-0000-0000-000000000001");
    private static final UUID SKU = UUID.fromString("aeaeaeae-0000-0000-0000-000000000002");
    private static final UUID WAREHOUSE = UUID.fromString("aeaeaeae-0000-0000-0000-000000000003");

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

    @Autowired
    OrderService orders;

    @Autowired
    com.fullmall.member.auth.MemberAuthService memberAuth;

    @Autowired
    JdbcTemplate jdbc;

    private String phone;
    private UUID batchId;

    @BeforeEach
    void seed() {
        phone = "139" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100_000_000));
        jdbc.update("""
                INSERT INTO market (code, name, currency, default_locale, active)
                VALUES ('RP', '真人市场', 'CNY', 'zh-CN', TRUE) ON CONFLICT (code) DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO warehouse (id, market_code, name, type, active)
                VALUES (?, 'RP', '真人仓', 'SELF', TRUE) ON CONFLICT (id) DO NOTHING
                """, WAREHOUSE);
        jdbc.update("""
                INSERT INTO product (id, title, status) VALUES (?, '真人商品', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
                """, PRODUCT);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, 'SKU-RP', 1000, 'CNY') ON CONFLICT (id) DO NOTHING
                """, SKU, PRODUCT);
        batchId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no,
                                            quantity, reserved_quantity, status)
                VALUES (?, ?, 'WAREHOUSE', ?, ?, 50, 0, 'AVAILABLE')
                """, batchId, SKU, WAREHOUSE, "RP-" + batchId);
    }

    private CheckoutRequest request(String otpCode) {
        return new CheckoutRequest(
                List.of(new CheckoutRequest.Item(SKU, 1)),
                FulfillmentMode.SHIP,
                null,
                new CheckoutRequest.Contact("真人用户", phone, null),
                new CheckoutRequest.Address("上海", "上海", "静安", "南京西路 1 号", "200040"),
                List.of(), null, null, null, new CheckoutRequest.CheckoutContext(null, null, otpCode, null, null));
    }

    @Test
    void unregisteredPhoneWithoutOtpIsRejected() {
        assertThatThrownBy(() -> orders.checkout(request(null), null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("验证码");
        // 没有产生订单
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE contact_phone = ?", Integer.class, phone)).isZero();
    }

    @Test
    void otpCreatesMemberAndBindsOrder() {
        String code = memberAuth.requestOtp(phone).code();

        var order = orders.checkout(request(code), null);

        UUID memberId = jdbc.queryForObject(
                "SELECT member_id FROM orders WHERE id = ?", UUID.class, order.id());
        assertThat(memberId).isNotNull();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM member WHERE phone = ?", Integer.class, phone)).isEqualTo(1);
    }

    @Test
    void staffPlacementSkipsOtpButRecordsOperator() {
        var staffRequest = new CheckoutRequest(
                List.of(new CheckoutRequest.Item(SKU, 1)),
                FulfillmentMode.SHIP,
                null,
                new CheckoutRequest.Contact("到店顾客", phone, null),
                new CheckoutRequest.Address("上海", "上海", "静安", "南京西路 2 号", "200040"),
                List.of(), null, null, null,
                new CheckoutRequest.CheckoutContext(null, null, null, "CASHIER", "收银员张三"));

        // 没有 OTP，但因为是员工代下单，允许成单
        var order = orders.checkout(staffRequest, null);

        assertThat(jdbc.queryForMap(
                "SELECT placed_by, placed_by_operator FROM orders WHERE id = ?", order.id()))
                .containsEntry("placed_by", "CASHIER")
                .containsEntry("placed_by_operator", "收银员张三");
    }

    @Test
    void staffPlacementRequiresOperator() {
        var staffRequest = new CheckoutRequest(
                List.of(new CheckoutRequest.Item(SKU, 1)),
                FulfillmentMode.SHIP,
                null,
                new CheckoutRequest.Contact("到店顾客", phone, null),
                new CheckoutRequest.Address("上海", "上海", "静安", "南京西路 2 号", "200040"),
                List.of(), null, null, null,
                new CheckoutRequest.CheckoutContext(null, null, null, "CASHIER", null));

        assertThatThrownBy(() -> orders.checkout(staffRequest, null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("操作员");
    }

    @Test
    void existingMemberPhoneNeedsNoOtp() {
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
                VALUES (?, 'M-RP-1', '真人老客', ?, 'STANDARD', 0, 0, 'CNY')
                """, UUID.randomUUID(), phone);

        var order = orders.checkout(request(null), null);

        assertThat(jdbc.queryForObject(
                "SELECT member_id FROM orders WHERE id = ?", UUID.class, order.id())).isNotNull();
    }
}
