package com.fullmall.ai.media.adapter;

import com.fullmall.ai.media.ProviderConfig;
import com.fullmall.platform.json.Json;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 embedding（`POST {endpoint}/embeddings`）——可直接指向本机 Ollama / vLLM。
 *
 * <p>用于知识库检索与"相似素材"推荐；返回向量以逗号分隔的浮点文本写入 TEXT 资产内容，
 * **不**做字符截断（否则向量失真）。
 */
public class OpenAiEmbeddingAdapter implements MediaGenerationAdapter {

    private final java.net.http.HttpClient http =
            com.fullmall.platform.http.HttpClients.shared();

    @Override
    public String protocol() {
        return "openai-embedding";
    }

    @Override
    public GeneratedMedia generate(ProviderConfig provider, String prompt, String negative,
                                   Map<String, Object> params) {
        String base = provider.endpoint().endsWith("/")
                ? provider.endpoint().substring(0, provider.endpoint().length() - 1)
                : provider.endpoint();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/embeddings"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + provider.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(
                        Json.write(Map.of("model", provider.model(), "input", prompt)),
                        StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("embedding 失败 HTTP " + response.statusCode());
            }
            var vector = Json.read(response.body()).path("data").path(0).path("embedding");
            if (!vector.isArray() || vector.isEmpty()) {
                throw new IllegalStateException("embedding 返回为空");
            }
            List<String> values = new ArrayList<>();
            vector.forEach(node -> values.add(String.valueOf(node.asDouble())));
            return GeneratedMedia.bytes(String.join(",", values).getBytes(StandardCharsets.UTF_8),
                    "text/plain");
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("embedding 请求失败：" + error.getMessage(), error);
        }
    }
}
