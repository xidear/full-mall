package com.fullmall.review.moderation.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.review.moderation.ContentModeration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 通用 HTTP 内容审核适配器（自建网关或供应商 REST）。
 * 契约：POST {text} -> {suggestion: PASS|REVIEW|BLOCK, reason}；非 200/未知值一律抛错。
 */
public class HttpModerationAdapter implements ContentModeration {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String endpoint;
    private final String apiKey;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    public HttpModerationAdapter(String endpoint, String apiKey) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("审核服务地址不能为空（app.review.moderation.endpoint）");
        }
        this.endpoint = endpoint.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public String provider() {
        return "http";
    }

    @Override
    public Verdict review(String text) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            MAPPER.writeValueAsString(Map.of("text", text == null ? "" : text)),
                            StandardCharsets.UTF_8));
            if (!apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }
            HttpResponse<String> response = http.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("审核服务返回 " + response.statusCode());
            }
            JsonNode body = MAPPER.readTree(response.body());
            String suggestion = body.path("suggestion").asText("");
            String reason = body.path("reason").asText("");
            return switch (suggestion.toUpperCase(java.util.Locale.ROOT)) {
                case "PASS" -> Verdict.approve();
                case "BLOCK" -> Verdict.reject(reason.isBlank() ? "供应商判定违规" : reason);
                case "REVIEW" -> Verdict.manual(reason.isBlank() ? "供应商建议人工复核" : reason);
                default -> throw new IllegalStateException("审核服务返回未知 suggestion：" + suggestion);
            };
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("调用审核服务失败：" + error.getMessage(), error);
        }
    }
}
