package com.fullmall.payment;

import com.fullmall.payment.wechat.WechatTransferClient;
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

/** 商家转账：签名可验证、报文含金额与 openid、错误码与缺 batch_id 都拒绝。 */
class WechatTransferClientTest {

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

    private String start(String body, AtomicReference<String> auth, AtomicReference<String> sent)
            throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/transfer/batches", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            sent.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void transfersWithVerifiableSignature() throws Exception {
        KeyPair pair = keyPair();
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> sent = new AtomicReference<>();
        String base = start("{\"batch_id\":\"B-1\",\"batch_status\":\"ACCEPTED\"}", auth, sent);
        var client = new WechatTransferClient("wx-app", "mch", "SERIAL1", pem(pair), API_V3_KEY, base);

        var transfer = client.transfer("WD-1", "openid-abc", 5000, "佣金提现");

        assertThat(transfer.batchId()).isEqualTo("B-1");
        assertThat(sent.get()).contains("\"openid\":\"openid-abc\"").contains("\"transfer_amount\":5000")
                .contains("\"total_num\":1");
        String signature = java.util.regex.Pattern.compile("signature=\"([^\"]+)\"")
                .matcher(auth.get()).results().findFirst().orElseThrow().group(1);
        String ts = java.util.regex.Pattern.compile("timestamp=\"([^\"]+)\"")
                .matcher(auth.get()).results().findFirst().orElseThrow().group(1);
        String nonce = java.util.regex.Pattern.compile("nonce_str=\"([^\"]+)\"")
                .matcher(auth.get()).results().findFirst().orElseThrow().group(1);
        String message = "POST\n/v3/transfer/batches\n" + ts + "\n" + nonce + "\n" + sent.get() + "\n";
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(pair.getPublic());
        verifier.update(message.getBytes(StandardCharsets.UTF_8));
        assertThat(verifier.verify(Base64.getDecoder().decode(signature))).isTrue();
    }

    @Test
    void rejectsMissingConfigAndInvalidResponses() throws Exception {
        assertThatThrownBy(() -> new WechatTransferClient("", "m", "s", "pem", API_V3_KEY, "http://x"))
                .hasMessageContaining("需配置");

        KeyPair pair = keyPair();
        var client = new WechatTransferClient("app", "mch", "S", pem(pair), API_V3_KEY,
                start("{\"batch_status\":\"ACCEPTED\"}", new AtomicReference<>(), new AtomicReference<>()));
        assertThatThrownBy(() -> client.transfer("WD-2", "openid", 100, null))
                .hasMessageContaining("缺少 batch_id");
        assertThatThrownBy(() -> client.transfer("WD-3", "openid", 0, null))
                .hasMessageContaining("必须大于 0");
    }
}
