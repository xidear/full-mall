package com.fullmall.ai.media.adapter;

import com.fullmall.ai.media.ProviderConfig;
import com.fullmall.platform.json.Json;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容对话（文案生成、提示词优化）。可直接指向 vLLM / Ollama / 兼容网关。
 */
public class OpenAiChatAdapter implements MediaGenerationAdapter {

    private final java.net.http.HttpClient http =
            com.fullmall.platform.http.HttpClients.shared();

    @Override
    public String protocol() {
        return "openai-chat";
    }

    @Override
    public GeneratedMedia generate(ProviderConfig provider, String prompt, String negative,
                                   Map<String, Object> params) {
        String system = params != null && params.get("system") != null
                ? String.valueOf(params.get("system")) : "你是电商文案助手，输出简洁中文，不编造参数。";
        String base = provider.endpoint().endsWith("/")
                ? provider.endpoint().substring(0, provider.endpoint().length() - 1)
                : provider.endpoint();
        Map<String, Object> body = Map.of(
                "model", provider.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", prompt)));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/chat/completions"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + provider.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(provider.code() + " 对话失败 HTTP " + response.statusCode());
            }
            String content = Json.read(response.body()).path("choices").path(0)
                    .path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new IllegalStateException(provider.code() + " 返回内容为空");
            }
            return GeneratedMedia.bytes(content.getBytes(StandardCharsets.UTF_8), "text/plain");
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("对话请求失败：" + error.getMessage(), error);
        }
    }
}
