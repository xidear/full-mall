package com.fullmall.payment;

import com.fullmall.order.app.OrderService;
import com.fullmall.payment.callback.PaymentCallbackController;
import com.fullmall.payment.callback.WechatV3CallbackVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 回调入账：验签通过→入账；重复通知幂等；金额不符拒绝。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class PaymentCallbackTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final String API_V3_KEY = "01234567890123456789012345678901";

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
    JdbcTemplate jdbc;

    private String order(String orderNo, long amount) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO orders (id, order_no, status, currency, subtotal_amount, shipping_fee_amount,
                                    discount_amount, total_amount, contact_name, contact_phone, fulfillment_mode)
                VALUES (?, ?, 'PENDING_PAYMENT', 'CNY', ?, 0, 0, ?, 'p', '13800000000', 'SHIP')
                """, id, orderNo, amount, amount);
        return orderNo;
    }

    @Test
    void marksPaidOnceAndRejectsAmountMismatch() {
        String orderNo = order("PCB-" + UUID.randomUUID().toString().substring(0, 8), 1990);

        assertThat(orders.markPaidByOrderNo(orderNo, 1990)).isTrue();
        assertThat(orders.markPaidByOrderNo(orderNo, 1990)).as("重复通知幂等").isFalse();
        assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE order_no = ?", String.class, orderNo))
                .isEqualTo("PAID");

        String other = order("PCB-" + UUID.randomUUID().toString().substring(0, 8), 1990);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                orders.markPaidByOrderNo(other, 100))
                .hasMessageContaining("不一致");
    }

    @Test
    void callbackEndpointRejectsForgedSignature() {
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
        KeyPair pair = generator.generateKeyPair();
        var verifier = new WechatV3CallbackVerifier(
                Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()), API_V3_KEY, 300);
        var controller = new PaymentCallbackController(verifier, orders);

        var response = controller.notify("1700000000", "nonce", "Zm9yZ2Vk", "{}");

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).containsEntry("code", "SIGNATURE_INVALID");
    }
}
