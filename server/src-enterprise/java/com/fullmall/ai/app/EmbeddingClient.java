package com.fullmall.ai.app;

import com.fullmall.ai.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的嵌入客户端（`POST /v1/embeddings`）。
 *
 * <p>任何非 200、缺字段、维度异常都**抛错**，不返回零向量假装成功——
 * 零向量会让检索静默失效，比报错更难查。
 */
public class EmbeddingClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final AiProperties properties;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public EmbeddingClient(AiProperties properties) {
        this.properties = properties;
    }

    public float[] embed(String text) {
        if (!properties.isReady()) {
            throw new IllegalStateException("AI 未就绪，无法计算嵌入（检查 app.ai.*）");
        }
        try {
            String body = MAPPER.writeValueAsString(Map.of(
                    "model", properties.getEmbeddingModel(),
                    "input", text));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEmbeddingBaseUrl().replaceAll("/$", "") + "/embeddings"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("嵌入服务返回 " + response.statusCode()
                        + "：" + response.body());
            }
            var json = MAPPER.readTree(response.body());
            var vectorNode = json.path("data").path(0).path("embedding");
            if (!vectorNode.isArray() || vectorNode.size() == 0) {
                throw new IllegalStateException("嵌入服务未返回向量：" + response.body());
            }
            float[] vector = new float[vectorNode.size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) vectorNode.get(i).asDouble();
            }
            return vector;
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("调用嵌入服务失败：" + error.getMessage(), error);
        }
    }
}
