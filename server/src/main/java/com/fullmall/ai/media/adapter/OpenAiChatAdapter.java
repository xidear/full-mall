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
 * OpenAI 兼容对话（文案生成、提示词优化、图片理解）。可直接指向 vLLM / Ollama / 兼容网关。
 *
 * <p>图片理解：params.images 传图片 URL 列表时，user 消息改成多模态 content
 * （{type:text} + {type:image_url}），兼容 Ollama 的 llava / moondream 等视觉模型。
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
        // 多模态：params.images 非空时改成 [{type:text},{type:image_url}...]
        List<?> imageUrls = params == null || !(params.get("images") instanceof List<?> list)
                ? List.of()
                : list;
        Object userContent;
        if (imageUrls.isEmpty()) {
            userContent = prompt;
        } else {
            List<Object> parts = new java.util.ArrayList<>();
            parts.add(Map.of("type", "text", "text", prompt));
            for (Object url : imageUrls) {
                parts.add(Map.of("type", "image_url",
                        "image_url", Map.of("url", String.valueOf(url))));
            }
            userContent = parts;
        }
        Map<String, Object> body = Map.of(
                "model", provider.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", userContent)));
        // 自营/本地模型可能排队较慢；默认 180s，可按供应商 extra.timeoutSeconds 覆盖。
        // 超出 5..600 直接失败，避免"配了 0 秒"变成永远超时或"配了 1 小时"占住线程。
        int timeoutSeconds = provider.extra() == null
                ? 180 : provider.extra().path("timeoutSeconds").asInt(180);
        if (timeoutSeconds < 5 || timeoutSeconds > 600) {
            throw new IllegalStateException("供应商 " + provider.code()
                    + " 的 timeoutSeconds 必须在 5..600，实际=" + timeoutSeconds);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/chat/completions"))
                .timeout(Duration.ofSeconds(timeoutSeconds))
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
