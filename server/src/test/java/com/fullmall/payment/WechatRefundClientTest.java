package com.fullmall.payment;

import com.fullmall.payment.wechat.WechatRefundClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 微信退款：签名可验证、金额校验、错误码不吞、缺 refund_id 视为失败。 */
class WechatRefundClientTest {

    private static final String API_V3_KEY = "01234567890123456789012345678901";
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String pem(KeyPair pair) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    private String start(String responseBody, AtomicReference<String> auth, AtomicReference<String> body)
            throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/refund/domestic/refunds", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void refundsWithVerifiableSignature() throws Exception {
        KeyPair pair = keyPair();
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        String base = start("{\"refund_id\":\"R-1\",\"status\":\"PROCESSING\"}", auth, body);
        var client = new WechatRefundClient("mch", "SERIAL1", pem(pair), API_V3_KEY,
                base, "https://shop.example.com/notify/refund");

        var refund = client.refund("SO-1", "RF-1", 500, 1990, "质量问题");

        assertThat(refund.refundId()).isEqualTo("R-1");
        assertThat(refund.status()).isEqualTo("PROCESSING");
        assertThat(body.get()).contains("\"out_refund_no\":\"RF-1\"").contains("\"refund\":500")
                .contains("\"total\":1990");

        String signatureValue = java.util.regex.Pattern.compile("signature=\"([^\"]+)\"")
                .matcher(auth.get()).results().findFirst().orElseThrow().group(1);
        String timestamp = java.util.regex.Pattern.compile("timestamp=\"([^\"]+)\"")
                .matcher(auth.get()).results().findFirst().orElseThrow().group(1);
        String nonce = java.util.regex.Pattern.compile("nonce_str=\"([^\"]+)\"")
                .matcher(auth.get()).results().findFirst().orElseThrow().group(1);
        String message = "POST\n/v3/refund/domestic/refunds\n" + timestamp + "\n" + nonce + "\n"
                + body.get() + "\n";
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(pair.getPublic());
        verifier.update(message.getBytes(StandardCharsets.UTF_8));
        assertThat(verifier.verify(Base64.getDecoder().decode(signatureValue))).isTrue();
    }

    @Test
    void validatesAmountsAndMissingRefundId() throws Exception {
        KeyPair pair = keyPair();
        String base = start("{\"status\":\"PROCESSING\"}", new AtomicReference<>(), new AtomicReference<>());
        var client = new WechatRefundClient("mch", "S", pem(pair), API_V3_KEY, base, "https://x/n");

        assertThatThrownBy(() -> client.refund("SO-1", "RF-1", 5000, 1990, null))
                .hasMessageContaining("不超过原订单金额");
        assertThatThrownBy(() -> client.refund("SO-1", "RF-1", 100, 1990, null))
                .hasMessageContaining("缺少 refund_id");
    }
}
