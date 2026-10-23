package com.fullmall.payment;

import com.fullmall.payment.wechat.WechatBillClient;
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

/** 账单下载：GET 签名含 query、反引号剥离、错误码不吞。 */
class WechatBillClientTest {

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

    private String start(String body, int status, AtomicReference<String> auth,
                         AtomicReference<String> rawPath) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/bill/tradebill", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            rawPath.set(exchange.getRequestURI().getRawPath() + "?"
                    + exchange.getRequestURI().getRawQuery());
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void downloadsAndSplitsCsvWithVerifiableSignature() throws Exception {
        KeyPair pair = keyPair();
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        String csv = "`交易单号`,`商户单号`,`金额`\n`4200001`,`SO-1`,`19.90`\n`4200002`,`SO-2`,`5.00`\n";
        var client = new WechatBillClient("mch", "SERIAL1", pem(pair), API_V3_KEY,
                start(csv, 200, auth, path));

        var bill = client.download("2026-10-20");

        assertThat(bill.header()).containsExactly("交易单号", "商户单号", "金额");
        assertThat(bill.rows()).hasSize(2);
        assertThat(bill.rows().get(0)).containsExactly("4200001", "SO-1", "19.90");
        assertThat(path.get()).contains("bill_date=2026-10-20").contains("bill_type=ALL");

        String signature = java.util.regex.Pattern.compile("signature=\"([^\"]+)\"")
                .matcher(auth.get()).results().findFirst().orElseThrow().group(1);
        String ts = java.util.regex.Pattern.compile("timestamp=\"([^\"]+)\"")
                .matcher(auth.get()).results().findFirst().orElseThrow().group(1);
        String nonce = java.util.regex.Pattern.compile("nonce_str=\"([^\"]+)\"")
                .matcher(auth.get()).results().findFirst().orElseThrow().group(1);
        String message = "GET\n" + path.get() + "\n" + ts + "\n" + nonce + "\n\n";
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(pair.getPublic());
        verifier.update(message.getBytes(StandardCharsets.UTF_8));
        assertThat(verifier.verify(Base64.getDecoder().decode(signature)))
                .as("签名必须覆盖含 query 的完整路径").isTrue();
    }

    @Test
    void rejectsBadDateAndHttpError() throws Exception {
        KeyPair pair = keyPair();
        var client = new WechatBillClient("mch", "S", pem(pair), API_V3_KEY,
                start("{\"code\":\"PARAM_ERROR\"}", 400, new AtomicReference<>(), new AtomicReference<>()));

        assertThatThrownBy(() -> client.download("2026/10/20"))
                .hasMessageContaining("格式必须为 yyyy-MM-dd");
        assertThatThrownBy(() -> client.download("2026-10-20"))
                .hasMessageContaining("账单下载失败 HTTP 400");
    }
}
