package com.fullmall.member;

import com.fullmall.fulfillment.app.FulfillmentService;
import com.fullmall.fulfillment.domain.FulfillmentStatus;
import com.fullmall.member.auth.MemberAuthService;
import com.fullmall.platform.error.ApiException;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 批次 F：邮箱登录与跨境清关状态。
 *
 * <p>两条红线：
 * <ul>
 *   <li>没绑定邮箱的账号不能用邮箱验证码登录——不临时造一条"没有手机号的会员"；</li>
 *   <li>没发货就登记"已清关"是假状态，必须拒绝。</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.member.otp.return-code=true"
        })
@ActiveProfiles("test")
class EmailLoginAndCustomsTest {

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

    @Autowired
    MemberAuthService auth;

    @Autowired
    FulfillmentService fulfillment;

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    com.fullmall.platform.secret.IntegrationConfigService configs;
    @Autowired
    com.fullmall.platform.channel.IntegrationRouteService routes;

    private static final String PHONE = "19900000001";
    private static final String EMAIL = "cross-border@example.com";

    @BeforeEach
    void setup() {
        // 邮箱验证码要真发出去：没有绑定「登录验证码（邮件）」就必然 503（ADR-0018），
        // 所以这里显式绑一个 mock 供应商 —— 演示占位不会联网，但通路是真的。
        configs.save("email", "mock", java.util.Map.of(), true, "tester");
        routes.bind("email", "OTP_LOGIN", "mock", null);
        jdbc.update("DELETE FROM member_otp");
        jdbc.update("DELETE FROM member_session");
        jdbc.update("DELETE FROM member WHERE phone = ?", PHONE);
    }

    @Test
    void emailLoginRequiresBoundEmail() {
        // 未绑定邮箱 → 明确报错，不临时创建无手机号会员
        assertThatThrownBy(() -> auth.requestEmailOtp(EMAIL))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("尚未绑定");
    }

    @Test
    void bindThenLoginByEmail() {
        var issued = auth.requestOtp(PHONE);
        var session = auth.verifyOtp(PHONE, issued.code());
        auth.bindEmail(session.memberId(), EMAIL);

        var emailIssued = auth.requestEmailOtp(EMAIL);
        assertThat(emailIssued.code()).isNotBlank();
        // 验证码必须**真的走邮件通道**（dev 下是 mock sender，落库为 SENT）；
        // 渠道没配或发不出去时会抛 EMAIL_SEND_FAILED，不会假装已发送
        var deliveries = jdbc.queryForList("""
                SELECT status FROM notification WHERE channel = 'EMAIL' AND recipient_id = ?
                """, String.class, EMAIL);
        assertThat(deliveries).isNotEmpty();
        assertThat(deliveries.get(deliveries.size() - 1)).isEqualTo("SENT");
        var emailSession = auth.verifyEmailOtp(EMAIL, emailIssued.code());
        assertThat(emailSession.memberId()).isEqualTo(session.memberId());
        assertThat(emailSession.token()).isNotBlank();
    }

    @Test
    void duplicateEmailIsRejected() {
        var first = auth.verifyOtp(PHONE, auth.requestOtp(PHONE).code());
        auth.bindEmail(first.memberId(), EMAIL);
        String otherPhone = "19900000002";
        jdbc.update("DELETE FROM member WHERE phone = ?", otherPhone);
        var second = auth.verifyOtp(otherPhone, auth.requestOtp(otherPhone).code());
        assertThatThrownBy(() -> auth.bindEmail(second.memberId(), EMAIL))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("已被其他会员绑定");
        jdbc.update("""
                DELETE FROM member_session WHERE member_id = ?
                """, second.memberId());
        jdbc.update("DELETE FROM member WHERE phone = ?", otherPhone);
    }

    @Test
    void customsCannotBeMarkedBeforeShipping() {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone,
                                    fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, '清关测试', '13800000002', 'SHIP')
                """, orderId, "CU-" + orderId);
        UUID fulfillmentId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO fulfillment_order (id, order_id, type, status) VALUES (?, ?, 'WAREHOUSE_SHIP', 'PICKING')
                """, fulfillmentId, orderId);
        assertThatThrownBy(() -> fulfillment.markCustoms(fulfillmentId, true, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("已发货");
    }

    @Test
    void customsAdvancesAfterShipping() {
        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone,
                                    fulfillment_mode)
                VALUES (?, ?, 'PAID', 'CNY', 1000, 0, 0, 1000, '清关测试2', '13800000003', 'SHIP')
                """, orderId, "CU2-" + orderId);
        UUID fulfillmentId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO fulfillment_order (id, order_id, type, status) VALUES (?, ?, 'WAREHOUSE_SHIP', 'SHIPPED')
                """, fulfillmentId, orderId);
        assertThat(fulfillment.markCustoms(fulfillmentId, false, "交承运商").status())
                .isEqualTo(FulfillmentStatus.CUSTOMS_CLEARING);
        assertThat(fulfillment.markCustoms(fulfillmentId, true, "已清关").status())
                .isEqualTo(FulfillmentStatus.CUSTOMS_CLEARED);
    }
}
