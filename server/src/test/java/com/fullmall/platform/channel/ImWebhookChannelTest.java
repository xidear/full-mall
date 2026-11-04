package com.fullmall.platform.channel;

import com.fullmall.platform.channel.sender.ChannelSettings;
import com.fullmall.platform.channel.sender.ImWebhookChannel;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 群机器人报文契约（**官方协议桩**，不发外网）：企微 markdown / 飞书 text / 钉钉 text + 加签。
 *
 * <p>为什么必须逐家断言：三家字段名不同（msgtype vs msg_type，content vs text），
 * 而且失败常常是 HTTP 200 + 业务错误码；只判状态码会让「消息根本没到群里」看起来像成功。
 */
class ImWebhookChannelTest {

    private HttpServer server;
    private final List<String> bodies = new CopyOnWriteArrayList<>();
    private final List<String> queries = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String responseBody = "{\"errcode\":0,\"errmsg\":\"ok\"}";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            queries.add(String.valueOf(exchange.getRequestURI().getQuery()));
            byte[] payload = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
    }

    @Test
    void wecomUsesMarkdownPayload() {
        new ImWebhookChannel("wecom", new ChannelSettings(Map.of("webhook-url", url())))
                .send("[FATAL] 支付回调丢失 · order-1");

        assertThat(bodies).hasSize(1);
        assertThat(bodies.get(0)).contains("\"msgtype\":\"markdown\"");
        assertThat(bodies.get(0)).contains("支付回调丢失");
    }

    @Test
    void feishuUsesTextPayload() {
        responseBody = "{\"code\":0,\"msg\":\"success\"}";
        new ImWebhookChannel("feishu", new ChannelSettings(Map.of("webhook-url", url())))
                .send("告警恢复");

        assertThat(bodies.get(0)).contains("\"msg_type\":\"text\"");
        assertThat(bodies.get(0)).contains("\"text\":\"告警恢复\"");
    }

    @Test
    void dingtalkSignsUrlWhenSecretConfigured() {
        new ImWebhookChannel("dingtalk", new ChannelSettings(Map.of(
                "webhook-url", url(), "secret", "SEC000")))
                .send("值班提醒");

        assertThat(queries.get(0)).contains("timestamp=").contains("sign=");
        assertThat(bodies.get(0)).contains("\"msgtype\":\"text\"");
    }

    @Test
    void businessErrorCodeIsFailureEvenWithHttp200() {
        responseBody = "{\"errcode\":310000,\"errmsg\":\"sign not match\"}";
        assertThatThrownBy(() -> new ImWebhookChannel("wecom",
                new ChannelSettings(Map.of("webhook-url", url()))).send("x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("310000");
    }

    @Test
    void unrecognizedResponseIsFailure() {
        responseBody = "<html>gateway error</html>";
        assertThatThrownBy(() -> new ImWebhookChannel("feishu",
                new ChannelSettings(Map.of("webhook-url", url()))).send("x"))
                .hasMessageContaining("UNRECOGNIZED_RESPONSE");
    }

    @Test
    void httpErrorIsFailure() {
        status = 502;
        responseBody = "bad gateway";
        assertThatThrownBy(() -> new ImWebhookChannel("dingtalk",
                new ChannelSettings(Map.of("webhook-url", url()))).send("x"))
                .hasMessageContaining("HTTP 502");
    }
}
