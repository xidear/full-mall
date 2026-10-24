package com.fullmall.ai.media.adapter;

import com.fullmall.ai.media.ProviderConfig;
import com.fullmall.platform.json.Json;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 火山方舟 Seedance（视频）：`content[]` + `role` 区分参考图/首帧/尾帧。
 *
 * <p>官方限制：**首帧、首尾帧、全模态参考**是三种互斥场景，不可混用。
 * 我们的策略：给了 FIRST_FRAME 就按首帧模式（忽略其它参考图的 role，仅文本引用）；
 * 否则全部按 `reference_image` 走全模态参考。多图不会被静默丢弃——会体现在 content 里。
 */
public class ArkVideoAdapter implements MediaGenerationAdapter {

    private final java.net.http.HttpClient http = com.fullmall.platform.http.HttpClients.shared();
    private final long pollIntervalMs;
    private final long timeoutMs;

    public ArkVideoAdapter() {
        this(5000, 600_000);
    }

    ArkVideoAdapter(long pollIntervalMs, long timeoutMs) {
        this.pollIntervalMs = pollIntervalMs;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String protocol() {
        return "ark-video";
    }

    @Override
    public GeneratedMedia generate(ProviderConfig provider, String prompt, String negative,
                                   Map<String, Object> params) {
        List<Map<String, Object>> images = images(params);
        boolean hasFirstFrame = images.stream().anyMatch(i -> "FIRST_FRAME".equals(i.get("role")));
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", prompt));
        for (Map<String, Object> image : images) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("type", "image_url");
            node.put("image_url", Map.of("url", image.get("url")));
            node.put("role", hasFirstFrame
                    ? ("FIRST_FRAME".equals(image.get("role")) ? "first_frame" : "reference_image")
                    : "reference_image");
            content.add(node);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", provider.model());
        body.put("content", content);
        if (params != null && params.get("duration") != null) {
            body.put("duration", params.get("duration"));
        }
        String base = trim(provider.endpoint());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/contents/generations/tasks"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + provider.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Seedance 提交失败 HTTP " + response.statusCode()
                        + "：" + abbreviate(response.body()));
            }
            String taskId = Json.read(response.body()).path("id").asText("");
            if (taskId.isBlank()) {
                throw new IllegalStateException("Seedance 提交未返回任务 id");
            }
            return GeneratedMedia.url(poll(base, provider, taskId), "video/mp4");
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("Seedance 请求失败：" + error.getMessage(), error);
        }
    }

    private String poll(String base, ProviderConfig provider, String taskId) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(pollIntervalMs);
            HttpResponse<String> response = http.send(HttpRequest.newBuilder()
                    .uri(URI.create(base + "/contents/generations/tasks/" + taskId))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + provider.apiKey())
                    .GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            var node = Json.read(response.body());
            String status = node.path("status").asText("");
            if ("succeeded".equalsIgnoreCase(status)) {
                String url = node.path("content").path("video_url").asText("");
                if (url.isBlank()) {
                    throw new IllegalStateException("Seedance 任务成功但缺少 video_url");
                }
                return url;
            }
            if ("failed".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
                throw new IllegalStateException("Seedance 任务失败："
                        + node.path("error").path("message").asText(""));
            }
        }
        throw new IllegalStateException("Seedance 任务超时（" + timeoutMs + "ms）");
    }

    private List<Map<String, Object>> images(Map<String, Object> params) {
        List<Map<String, Object>> images = new ArrayList<>();
        if (params == null) {
            return images;
        }
        if (params.get("images") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && map.get("url") != null) {
                    Map<String, Object> image = new LinkedHashMap<>();
                    image.put("url", String.valueOf(map.get("url")));
                    image.put("role", map.get("role") == null ? "REFERENCE"
                            : String.valueOf(map.get("role")));
                    images.add(image);
                }
            }
        }
        return images;
    }

    private String trim(String endpoint) {
        return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    private String abbreviate(String text) {
        return text == null ? "" : (text.length() > 300 ? text.substring(0, 300) : text);
    }
}
