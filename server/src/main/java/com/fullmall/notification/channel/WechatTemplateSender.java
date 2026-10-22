package com.fullmall.notification.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.notification.domain.Notification;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * 微信模板消息（公众号）。
 *
 * <p>微信**没有官方 Java SDK**（官方只提供 HTTP 接口），因此这里用 JDK HttpClient 直连：
 * 先取 `access_token`（缓存 7000 秒，官方有效期 7200），再发模板消息。
 * 这属于"没有成熟 SDK 可用"的例外，已在文档说明。
 *
 * <p>配置缺失或发送失败一律抛错，不返回成功。
 */
public class WechatTemplateSender implements NotificationChannelSender {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String appId;
    private final String appSecret;
    private final String templateId;
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    private String accessToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public WechatTemplateSender(String appId, String appSecret, String templateId, String baseUrl) {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()
                || templateId == null || templateId.isBlank()) {
            throw new IllegalStateException(
                    "app.notification.wechat 需配置 app-id/app-secret/template-id");
        }
        this.appId = appId;
        this.appSecret = appSecret;
        this.templateId = templateId;
        this.baseUrl = baseUrl == null || baseUrl.isBlank()
                ? "https://api.weixin.qq.com" : baseUrl;
    }

    @Override
    public String channel() {
        return "WECHAT";
    }

    @Override
    public void send(Notification notification) {
        try {
            String token = token();
            String payload = MAPPER.writeValueAsString(Map.of(
                    "touser", notification.recipientId(),
                    "template_id", templateId,
                    "data", Map.of("content", Map.of("value", notification.content()))));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/cgi-bin/message/template/send?access_token=" + token))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("微信模板消息 HTTP " + response.statusCode());
            }
            int errcode = MAPPER.readTree(response.body()).path("errcode").asInt(-1);
            if (errcode != 0) {
                throw new IllegalStateException("微信模板消息失败 errcode=" + errcode
                        + "：" + response.body());
            }
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("发送微信模板消息失败：" + error.getMessage(), error);
        }
    }

    private synchronized String token() throws Exception {
        if (accessToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return accessToken;
        }
        String url = baseUrl + "/cgi-bin/token?grant_type=client_credential&appid=" + appId
                + "&secret=" + appSecret;
        HttpResponse<String> response = http.send(HttpRequest.newBuilder().uri(URI.create(url))
                .timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        var node = MAPPER.readTree(response.body());
        String token = node.path("access_token").asText(null);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("获取微信 access_token 失败：" + response.body());
        }
        accessToken = token;
        tokenExpiresAt = Instant.now().plusSeconds(7000);
        return token;
    }
}
