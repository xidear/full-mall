package com.fullmall.analytics.ai;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的文本/生图客户端（可对接内网 vLLM、Ollama、兼容网关）。
 * 非 2xx、空 choices、空 url 一律抛错，不返回占位内容。
 */
public class OpenAiCompatClient {

    private static final ObjectMapper MAPPER = com.fullmall.platform.json.Json.mapper();

    private final String baseUrl;
    private final String apiKey;
    private final String textModel;
    private final String imageModel;
    private final HttpClient http = com.fullmall.platform.http.HttpClients.shared();

    public OpenAiCompatClient(String baseUrl, String apiKey, String textModel, String imageModel) {
        if (isBlank(baseUrl) || isBlank(apiKey) || isBlank(textModel)) {
            throw new IllegalStateException(
                    "AI 接口需配置 base-url/api-key/text-model（生图另需 image-model）");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.textModel = textModel;
        this.imageModel = imageModel == null ? "" : imageModel.trim();
    }

    public boolean imageConfigured() {
        return !imageModel.isBlank();
    }

    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", textModel);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
                Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt)));
        payload.put("temperature", 0.7);
        String body = post("/chat/completions", payload);
        try {
            var node = MAPPER.readTree(body).path("choices");
            if (!node.isArray() || node.isEmpty()) {
                throw new IllegalStateException("AI 返回缺少 choices：" + body);
            }
            String content = node.get(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new IllegalStateException("AI 返回内容为空");
            }
            return content;
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("AI 响应解析失败", error);
        }
    }

    public String image(String prompt) {
        if (!imageConfigured()) {
            throw new IllegalStateException("未配置 image-model，无法生成图片");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", imageModel);
        payload.put("prompt", prompt == null ? "" : prompt);
        payload.put("n", 1);
        String body = post("/images/generations", payload);
        try {
            var data = MAPPER.readTree(body).path("data");
            if (!data.isArray() || data.isEmpty()) {
                throw new IllegalStateException("生图返回缺少 data：" + body);
            }
            String url = data.get(0).path("url").asText("");
            if (url.isBlank()) {
                throw new IllegalStateException("生图返回缺少 url");
            }
            return url;
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("生图响应解析失败", error);
        }
    }

    private String post(String path, Map<String, Object> payload) {
        String body;
        try {
            body = MAPPER.writeValueAsString(payload);
        } catch (Exception error) {
            throw new IllegalStateException("AI 请求序列化失败", error);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("AI 接口失败 HTTP " + response.statusCode()
                        + "：" + response.body());
            }
            return response.body();
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("AI 请求失败：" + error.getMessage(), error);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
