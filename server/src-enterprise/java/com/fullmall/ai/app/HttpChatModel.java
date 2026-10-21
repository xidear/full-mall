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
 * OpenAI 兼容对话模型客户端。
 *
 * <p>失败语义：非 200、无 choices、内容为空都**抛错**（由上游转成 503），
 * 不返回"抱歉我没理解"之类伪造回答——那会让客服误以为是模型答的。
 */
public class HttpChatModel implements ChatModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final AiProperties properties;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public HttpChatModel(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public String complete(String system, String user) {
        if (!properties.isReady()) {
            throw new IllegalStateException("AI 未就绪（检查 app.ai.chat-* 配置）");
        }
        try {
            String body = MAPPER.writeValueAsString(Map.of(
                    "model", properties.getChatModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user)),
                    "temperature", 0.2,
                    "max_tokens", properties.getChatMaxTokens()));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getChatBaseUrl().replaceAll("/$", "") + "/chat/completions"))
                    .timeout(Duration.ofSeconds(properties.getChatTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getChatApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("对话服务返回 " + response.statusCode()
                        + "：" + response.body());
            }
            var content = MAPPER.readTree(response.body())
                    .path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException("对话服务返回空内容：" + response.body());
            }
            return content.asText();
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("调用对话服务失败：" + error.getMessage(), error);
        }
    }
}
