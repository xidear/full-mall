package com.fullmall.analytics;

import com.fullmall.analytics.ai.OpenAiCompatClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** OpenAI 兼容客户端：解析 choices/url、错误码不吞、缺配置失败。 */
class OpenAiCompatClientTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String start(String body, int status, AtomicReference<String> auth,
                         AtomicReference<String> path) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            path.set(exchange.getRequestURI().getPath());
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void parsesChatAndImage() throws Exception {
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        String base = start("{\"choices\":[{\"message\":{\"content\":\"夏季清爽上市\"}}]}",
                200, auth, path);
        var client = new OpenAiCompatClient(base, "sk-test", "gpt-4o-mini", "dall-e-3");

        assertThat(client.chat("系统", "写一句")).isEqualTo("夏季清爽上市");
        assertThat(auth.get()).isEqualTo("Bearer sk-test");
        assertThat(path.get()).isEqualTo("/chat/completions");

        String imageBase = start("{\"data\":[{\"url\":\"https://cdn.example.com/a.png\"}]}",
                200, auth, path);
        var imageClient = new OpenAiCompatClient(imageBase, "sk-test", "gpt-4o-mini", "dall-e-3");
        assertThat(imageClient.image("白底商品图")).startsWith("https://cdn.example.com/");
    }

    @Test
    void rejectsErrorsAndMissingConfig() throws Exception {
        assertThatThrownBy(() -> new OpenAiCompatClient("", "k", "m", ""))
                .hasMessageContaining("需配置");

        String base = start("{\"error\":\"bad\"}", 500, new AtomicReference<>(), new AtomicReference<>());
        var client = new OpenAiCompatClient(base, "sk", "model", "");
        assertThatThrownBy(() -> client.chat("s", "u"))
                .hasMessageContaining("AI 接口失败 HTTP 500");
        assertThatThrownBy(() -> client.image("x"))
                .hasMessageContaining("未配置 image-model");

        String empty = start("{\"choices\":[]}", 200, new AtomicReference<>(), new AtomicReference<>());
        var emptyClient = new OpenAiCompatClient(empty, "sk", "model", "");
        assertThatThrownBy(() -> emptyClient.chat("s", "u"))
                .hasMessageContaining("缺少 choices");
    }
}
