package com.fullmall.logistics;

import com.fullmall.logistics.app.Kuaidi100Client;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 快递100 HTTP 对接（mock 服务器）：
 * 请求体必须包含 customer/sign/param，签名与 {@code Kuaidi100Query} 一致；未配置时明确拒绝。
 */
class Kuaidi100ClientTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String startStub(AtomicReference<String> captured) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/poll/query.do", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            captured.set(body);
            byte[] response = "{\"message\":\"ok\",\"status\":\"200\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/poll/query.do";
    }

    @Test
    void sendsSignedFormAndReturnsBody() throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        String endpoint = startStub(captured);
        var client = new Kuaidi100Client("cust-1", "key-1", endpoint);

        String body = client.query("SF", "SF123456789");

        assertThat(body).contains("\"status\":\"200\"");
        String sent = captured.get();
        assertThat(sent).contains("customer=cust-1");
        assertThat(sent).contains("sign=");
        assertThat(sent).contains("param=");
        assertThat(sent).doesNotContain("key-1");   // 密钥绝不能出现在请求体里
    }

    @Test
    void unconfiguredQueryIsRejectedExplicitly() {
        var client = new Kuaidi100Client("", "", "http://127.0.0.1:1/poll/query.do");

        assertThat(client.configured()).isFalse();
        assertThatThrownBy(() -> client.query("SF", "SF1"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("未配置");
    }
}
