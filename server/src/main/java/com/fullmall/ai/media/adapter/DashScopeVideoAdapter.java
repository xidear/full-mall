package com.fullmall.ai.media.adapter;

import com.fullmall.ai.media.ProviderConfig;
import com.fullmall.platform.json.Json;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 阿里云百炼视频生成（图生视频/文生视频）：异步任务提交 + 轮询。
 * 输入可带首帧图 URL（`img_url`），用于"商品主图 → 短视频"。
 */
public class DashScopeVideoAdapter implements MediaGenerationAdapter {

    private final java.net.http.HttpClient http =
            com.fullmall.platform.http.HttpClients.shared();
    private final long pollIntervalMs;
    private final long timeoutMs;

    public DashScopeVideoAdapter() {
        this(5000, 600_000);
    }

    DashScopeVideoAdapter(long pollIntervalMs, long timeoutMs) {
        this.pollIntervalMs = pollIntervalMs;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String protocol() {
        return "dashscope-video";
    }

    @Override
    public GeneratedMedia generate(ProviderConfig provider, String prompt, String negative,
                                   Map<String, Object> params) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("prompt", prompt);
        if (params != null && params.get("firstFrameUrl") != null) {
            input.put("img_url", params.get("firstFrameUrl"));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", provider.model());
        body.put("input", input);
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (params != null && params.get("duration") != null) {
            parameters.put("duration", params.get("duration"));
        }
        if (params != null && params.get("size") != null) {
            parameters.put("size", params.get("size"));
        }
        body.put("parameters", parameters);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(provider.endpoint()))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + provider.apiKey())
                .header("X-DashScope-Async", "enable")
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("视频提交失败 HTTP " + response.statusCode());
            }
            String taskId = Json.read(response.body()).path("output").path("task_id").asText("");
            if (taskId.isBlank()) {
                throw new IllegalStateException("视频提交未返回 task_id");
            }
            return GeneratedMedia.url(poll(provider, taskId), "video/mp4");
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("视频生成失败：" + error.getMessage(), error);
        }
    }

    private String poll(ProviderConfig provider, String taskId) throws Exception {
        String endpoint = provider.endpoint();
        int services = endpoint.indexOf("/services/");
        String base = services > 0 ? endpoint.substring(0, services) : "https://dashscope.aliyuncs.com/api/v1";
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(pollIntervalMs);
            HttpResponse<String> response = http.send(HttpRequest.newBuilder()
                    .uri(URI.create(base + "/api/v1/tasks/" + taskId))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + provider.apiKey())
                    .GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            var output = Json.read(response.body()).path("output");
            String status = output.path("task_status").asText("");
            if ("SUCCEEDED".equals(status)) {
                String url = output.path("video_url").asText("");
                if (url.isBlank()) {
                    throw new IllegalStateException("视频任务成功但缺少 video_url");
                }
                return url;
            }
            if ("FAILED".equals(status) || "CANCELED".equals(status)) {
                throw new IllegalStateException("视频任务失败：" + output.path("message").asText(""));
            }
        }
        throw new IllegalStateException("视频任务超时（" + timeoutMs + "ms）");
    }
}
