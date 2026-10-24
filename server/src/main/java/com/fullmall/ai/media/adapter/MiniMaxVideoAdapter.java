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
 * MiniMax 海螺视频（H3 参考生视频）：首帧图 + `subject_reference`（角色/主体参考，可多张）。
 *
 * <p>官方语义：参考生视频支持多图/多视频混合参考；`subject_reference` 用于"角色脸部定型"。
 * 我们只传图片类参考（视频参考在素材库暂不支持），第一张 SUBJECT 角色图作为主体定型。
 *
 * <p>异步：提交拿 task_id，轮询 `GET {base}/query/video_generation?task_id=`，
 * 成功后从 `file_id` 再取下载地址（官方两段式）。
 */
public class MiniMaxVideoAdapter implements MediaGenerationAdapter {

    private final java.net.http.HttpClient http = com.fullmall.platform.http.HttpClients.shared();
    private final long pollIntervalMs;
    private final long timeoutMs;

    public MiniMaxVideoAdapter() {
        this(5000, 600_000);
    }

    MiniMaxVideoAdapter(long pollIntervalMs, long timeoutMs) {
        this.pollIntervalMs = pollIntervalMs;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String protocol() {
        return "minimax-video";
    }

    @Override
    public GeneratedMedia generate(ProviderConfig provider, String prompt, String negative,
                                   Map<String, Object> params) {
        List<Map<String, Object>> images = images(params);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", provider.model());
        body.put("prompt", prompt);
        if (params != null && params.get("duration") != null) {
            body.put("duration", params.get("duration"));
        }
        // 首帧：显式 FIRST_FRAME 优先，否则用第一张参考图
        images.stream().filter(i -> "FIRST_FRAME".equals(i.get("role"))).findFirst()
                .or(() -> images.stream().findFirst())
                .ifPresent(i -> body.put("first_frame_image", i.get("url")));
        // 角色脸部定型：SUBJECT 图放进 subject_reference（可多张）
        List<Map<String, Object>> subjects = new ArrayList<>();
        for (Map<String, Object> image : images) {
            if ("SUBJECT".equals(image.get("role"))) {
                subjects.add(Map.of("type", "character", "image", image.get("url")));
            }
        }
        if (!subjects.isEmpty()) {
            body.put("subject_reference", subjects);
        }

        String base = trim(provider.endpoint());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/video_generation"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + provider.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("MiniMax 提交失败 HTTP " + response.statusCode()
                        + "：" + abbreviate(response.body()));
            }
            String taskId = Json.read(response.body()).path("task_id").asText("");
            if (taskId.isBlank()) {
                throw new IllegalStateException("MiniMax 提交未返回 task_id");
            }
            return GeneratedMedia.url(poll(base, provider, taskId), "video/mp4");
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("MiniMax 请求失败：" + error.getMessage(), error);
        }
    }

    private String poll(String base, ProviderConfig provider, String taskId) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(pollIntervalMs);
            HttpResponse<String> response = http.send(HttpRequest.newBuilder()
                    .uri(URI.create(base + "/query/video_generation?task_id=" + taskId))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + provider.apiKey())
                    .GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            var root = Json.read(response.body());
            String status = root.path("status").asText("");
            if ("Success".equalsIgnoreCase(status)) {
                String url = root.path("file_url").asText("");
                if (url.isBlank()) {
                    String fileId = root.path("file_id").asText("");
                    if (fileId.isBlank()) {
                        throw new IllegalStateException("MiniMax 任务成功但缺少 file_id/file_url");
                    }
                    url = retrieveFile(base, provider, fileId);
                }
                return url;
            }
            if ("Fail".equalsIgnoreCase(status)) {
                throw new IllegalStateException("MiniMax 任务失败："
                        + root.path("base_resp").path("status_msg").asText(""));
            }
        }
        throw new IllegalStateException("MiniMax 任务超时（" + timeoutMs + "ms）");
    }

    private String retrieveFile(String base, ProviderConfig provider, String fileId) throws Exception {
        HttpResponse<String> response = http.send(HttpRequest.newBuilder()
                .uri(URI.create(base + "/files/retrieve?file_id=" + fileId))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + provider.apiKey())
                .GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String url = Json.read(response.body()).path("file").path("download_url").asText("");
        if (url.isBlank()) {
            throw new IllegalStateException("MiniMax 取文件下载地址失败");
        }
        return url;
    }

    private List<Map<String, Object>> images(Map<String, Object> params) {
        List<Map<String, Object>> images = new ArrayList<>();
        if (params == null || !(params.get("images") instanceof List<?> list)) {
            return images;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map && map.get("url") != null) {
                Map<String, Object> image = new LinkedHashMap<>();
                image.put("url", String.valueOf(map.get("url")));
                image.put("role", map.get("role") == null ? "REFERENCE" : String.valueOf(map.get("role")));
                images.add(image);
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
