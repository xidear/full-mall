package com.fullmall.payment;

import com.fullmall.payment.alipay.AlipayCallbackVerifier;
import com.fullmall.payment.alipay.AlipayRefundClient;
import com.fullmall.payment.alipay.AlipaySigner;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 支付宝退款与回调验签：响应必须验签、回调伪造必须拒绝。 */
class AlipayRefundAndCallbackTest {

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

    private String priv(KeyPair pair) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    private String pub(KeyPair pair) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }

    private String start(String json) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/gateway.do", exchange -> {
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/gateway.do";
    }

    @Test
    void refundVerifiesResponseSignature() throws Exception {
        KeyPair app = keyPair();
        KeyPair alipay = keyPair();
        String node = "{\"code\":\"10000\",\"trade_no\":\"T-9\",\"fund_change\":\"Y\"}";
        java.security.Signature signer = java.security.Signature.getInstance("SHA256withRSA");
        signer.initSign(alipay.getPrivate());
        signer.update(node.getBytes(StandardCharsets.UTF_8));
        String signed = "{\"alipay_trade_refund_response\":" + node + ",\"sign\":\""
                + Base64.getEncoder().encodeToString(signer.sign()) + "\"}";
        var client = new AlipayRefundClient("app-1", priv(app), pub(alipay), start(signed));

        var refund = client.refund("SO-1", "RF-1", 500, "质量问题");

        assertThat(refund.tradeNo()).isEqualTo("T-9");
        assertThat(refund.fundChange()).isEqualTo("Y");
    }

    @Test
    void callbackRejectsForgeryAndAcceptsValid() throws Exception {
        KeyPair alipay = keyPair();
        var verifier = new AlipayCallbackVerifier(pub(alipay), "app-1");

        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", "app-1");
        params.put("out_trade_no", "SO-1");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("total_amount", "19.90");
        String content = AlipaySigner.signContent(params);
        java.security.Signature signer = java.security.Signature.getInstance("SHA256withRSA");
        signer.initSign(alipay.getPrivate());
        signer.update(content.getBytes(StandardCharsets.UTF_8));
        params.put("sign", Base64.getEncoder().encodeToString(signer.sign()));
        params.put("sign_type", "RSA2");

        var ok = verifier.verify(params);
        assertThat(ok.valid()).isTrue();
        assertThat(ok.totalFen()).isEqualTo(1990L);

        var tampered = new LinkedHashMap<>(params);
        tampered.put("total_amount", "0.01");
        assertThat(verifier.verify(tampered).valid()).isFalse();

        // 另一个"合法签名但属于别家应用"的通知：平台密钥相同，app_id 不同
        Map<String, String> otherApp = new LinkedHashMap<>();
        otherApp.put("app_id", "other-app");
        otherApp.put("out_trade_no", "SO-9");
        otherApp.put("trade_status", "TRADE_SUCCESS");
        otherApp.put("total_amount", "1.00");
        java.security.Signature otherSigner = java.security.Signature.getInstance("SHA256withRSA");
        otherSigner.initSign(alipay.getPrivate());
        otherSigner.update(AlipaySigner.signContent(otherApp).getBytes(StandardCharsets.UTF_8));
        otherApp.put("sign", Base64.getEncoder().encodeToString(otherSigner.sign()));
        assertThat(verifier.verify(otherApp).reason()).contains("app_id 不匹配");

        assertThatThrownBy(() -> new AlipayCallbackVerifier("", "app-1"))
                .hasMessageContaining("公钥未配置");
    }
}
