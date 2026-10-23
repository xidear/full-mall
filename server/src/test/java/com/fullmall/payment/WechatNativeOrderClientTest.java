package com.fullmall.payment;

import com.fullmall.payment.wechat.WechatNativeOrderClient;
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

/** 微信 Native 下单：Authorization 签名可被公钥验证、缺配置失败、错误码不被吞。 */
class WechatNativeOrderClientTest {

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

    private String start(String responseBody, int status, AtomicReference<String> auth,
                         AtomicReference<String> body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/pay/transactions/native", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void signsRequestAndParsesCodeUrl() throws Exception {
        KeyPair pair = keyPair();
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        String base = start("{\"code_url\":\"weixin://wxpay/bizpayurl?pr=abc\"}", 200, auth, body);
        var client = new WechatNativeOrderClient("wx-app-1", "1900000109", "SERIAL1", pem(pair),
                API_V3_KEY, base, "https://shop.example.com/api/v1/payments/wechat/notify");

        var order = client.create("SO-1001", 1990, "测试商品");

        assertThat(order.codeUrl()).startsWith("weixin://");
        assertThat(body.get()).contains("\"out_trade_no\":\"SO-1001\"").contains("\"total\":1990")
                .contains("\"appid\":\"wx-app-1\"");
        assertThat(auth.get()).startsWith("WECHATPAY2-SHA256-RSA2048 ").contains("mchid=\"1900000109\"");

        // 用公钥复算签名，证明 Authorization 里的 signature 是真实可验证的
        String signatureValue = java.util.regex.Pattern
                .compile("signature=\"([^\"]+)\"")
                .matcher(auth.get()).results().findFirst().orElseThrow().group(1);
        String timestamp = java.util.regex.Pattern
                .compile("timestamp=\"([^\"]+)\"")
                .matcher(auth.get()).results().findFirst().orElseThrow().group(1);
        String nonce = java.util.regex.Pattern
                .compile("nonce_str=\"([^\"]+)\"")
                .matcher(auth.get()).results().findFirst().orElseThrow().group(1);
        String message = "POST\n/v3/pay/transactions/native\n" + timestamp + "\n" + nonce + "\n"
                + body.get() + "\n";
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(pair.getPublic());
        verifier.update(message.getBytes(StandardCharsets.UTF_8));
        assertThat(verifier.verify(Base64.getDecoder().decode(signatureValue))).isTrue();
    }

    @Test
    void failsFastOnMissingConfigAndPropagatesErrors() throws Exception {
        assertThatThrownBy(() -> new WechatNativeOrderClient("", "mch", "S", "PEM", API_V3_KEY, "http://x", "http://y"))
                .hasMessageContaining("需配置");

        KeyPair pair = keyPair();
        String base = start("{\"code\":\"PARAM_ERROR\",\"message\":\"appid missing\"}", 400,
                new AtomicReference<>(), new AtomicReference<>());
        var client = new WechatNativeOrderClient("wx-app-1", "mch", "S", pem(pair), API_V3_KEY, base, "https://x/notify");

        assertThatThrownBy(() -> client.create("SO-2", 100, "x"))
                .hasMessageContaining("微信下单失败 HTTP 400")
                .hasMessageContaining("appid missing");
    }
}
