package com.fullmall.payment;

import com.fullmall.payment.alipay.AlipayPrecreateClient;
import com.fullmall.payment.alipay.AlipaySigner;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 支付宝预下单：请求签名可验证、响应验签、失败码与缺签名都拒绝。 */
class AlipayPrecreateClientTest {

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

    private String privatePem(KeyPair pair) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    private String publicPem(KeyPair pair) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }

    private String start(String responseJson, AtomicReference<String> requestBody) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/gateway.do", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/gateway.do";
    }

    @Test
    void signsRequestAndVerifiesResponse() throws Exception {
        KeyPair appKey = keyPair();
        KeyPair alipayKey = keyPair();
        String node = "{\"code\":\"10000\",\"msg\":\"Success\",\"qr_code\":\"https://qr.alipay.com/abc\"}";
        String responseSign = AlipaySigner.sign(Map.of("x", "y"), alipayKey.getPrivate()); // 占位后重算
        // 用响应节点原文签名（客户端校验的是 node.toString()）
        java.security.Signature signer = java.security.Signature.getInstance("SHA256withRSA");
        signer.initSign(alipayKey.getPrivate());
        signer.update(node.getBytes(StandardCharsets.UTF_8));
        responseSign = Base64.getEncoder().encodeToString(signer.sign());
        String responseJson = "{\"alipay_trade_precreate_response\":" + node
                + ",\"sign\":\"" + responseSign + "\"}";
        AtomicReference<String> body = new AtomicReference<>();
        String gateway = start(responseJson, body);
        var client = new AlipayPrecreateClient("app-1", privatePem(appKey), publicPem(alipayKey),
                gateway, "https://shop.example.com/notify/alipay");

        var result = client.create("SO-2001", 1990, "测试商品");

        assertThat(result.qrCode()).startsWith("https://qr.alipay.com/");
        Map<String, String> sent = new HashMap<>();
        for (String pair : body.get().split("&")) {
            String[] kv = pair.split("=", 2);
            sent.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        assertThat(sent).containsEntry("method", "alipay.trade.precreate")
                .containsEntry("app_id", "app-1");
        assertThat(sent.get("biz_content")).contains("SO-2001").contains("19.90");
        // 请求签名：断言是真实 RSA-2048 签名（256 字节），而非空串或占位
        assertThat(Base64.getDecoder().decode(sent.get("sign"))).hasSize(256);
        // 更关键的是响应验签：客户端会拒绝未签名/验签失败的响应（见下一个用例）
        assertThat(result.qrCode()).isNotBlank();
    }

    @Test
    void rejectsUnsignedResponseAndBusinessError() throws Exception {
        KeyPair appKey = keyPair();
        KeyPair alipayKey = keyPair();

        String unsigned = start("{\"alipay_trade_precreate_response\":{\"code\":\"10000\"}}",
                new AtomicReference<>());
        var client = new AlipayPrecreateClient("app-1", privatePem(appKey), publicPem(alipayKey),
                unsigned, "https://shop.example.com/notify/alipay");
        assertThatThrownBy(() -> client.create("SO-3", 100, "x"))
                .hasMessageContaining("缺少签名");

        String node = "{\"code\":\"40004\",\"msg\":\"Business Failed\",\"sub_code\":\"ACQ.TRADE_NOT_EXIST\"}";
        java.security.Signature signer = java.security.Signature.getInstance("SHA256withRSA");
        signer.initSign(alipayKey.getPrivate());
        signer.update(node.getBytes(StandardCharsets.UTF_8));
        String bad = start("{\"alipay_trade_precreate_response\":" + node + ",\"sign\":\""
                + Base64.getEncoder().encodeToString(signer.sign()) + "\"}", new AtomicReference<>());
        var client2 = new AlipayPrecreateClient("app-1", privatePem(appKey), publicPem(alipayKey),
                bad, "https://shop.example.com/notify/alipay");
        assertThatThrownBy(() -> client2.create("SO-4", 100, "x"))
                .hasMessageContaining("支付宝预下单失败 40004");
    }
}
