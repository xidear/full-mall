package com.fullmall.ai.media.adapter;

import com.fullmall.ai.media.ProviderConfig;
import com.fullmall.platform.json.Json;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云百炼（DashScope）文生图：**异步任务**提交 + 轮询。
 *
 * <p>与 OpenAI 协议不同：`X-DashScope-Async: enable` 返回 `task_id`，
 * 需 GET `/api/v1/tasks/{task_id}` 直到 `SUCCEEDED`，再从 `output.results[].url` 取图。
 */
public class DashScopeImageAdapter implements MediaGenerationAdapter {

    private final java.net.http.HttpClient http =
            com.fullmall.platform.http.HttpClients.shared();
    private final long pollIntervalMs;
    private final long timeoutMs;

    public DashScopeImageAdapter() {
        this(2000, 180_000);
    }

    DashScopeImageAdapter(long pollIntervalMs, long timeoutMs) {
        this.pollIntervalMs = pollIntervalMs;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String protocol() {
        return "dashscope-image";
    }

    @Override
    public GeneratedMedia generate(ProviderConfig provider, String prompt, String negative,
                                   Map<String, Object> params) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("prompt", prompt);
        if (negative != null && !negative.isBlank()) {
            input.put("negative_prompt", negative);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", provider.model());
        body.put("input", input);
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("n", 1);
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
                throw new IllegalStateException("DashScope 提交失败 HTTP " + response.statusCode()
                        + "：" + abbreviate(response.body()));
            }
            String taskId = Json.read(response.body()).path("output").path("task_id").asText("");
            if (taskId.isBlank()) {
                throw new IllegalStateException("DashScope 提交未返回 task_id：" + abbreviate(response.body()));
            }
            String url = poll(provider, taskId);
            return GeneratedMedia.url(url, "image/png");
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("DashScope 生图失败：" + error.getMessage(), error);
        }
    }

    private String poll(ProviderConfig provider, String taskId) throws Exception {
        String tasksUrl = tasksEndpoint(provider);
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(pollIntervalMs);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tasksUrl + "/" + taskId))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + provider.apiKey())
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("DashScope 查询任务失败 HTTP " + response.statusCode());
            }
            var node = Json.read(response.body()).path("output");
            String status = node.path("task_status").asText("");
            if ("SUCCEEDED".equals(status)) {
                var results = node.path("results");
                if (!results.isArray() || results.isEmpty()) {
                    throw new IllegalStateException("DashScope 任务成功但没有结果");
                }
                String url = results.get(0).path("url").asText("");
                if (url.isBlank()) {
                    throw new IllegalStateException("DashScope 结果缺少 url");
                }
                return url;
            }
            if ("FAILED".equals(status) || "CANCELED".equals(status) || "UNKNOWN".equals(status)) {
                throw new IllegalStateException("DashScope 任务失败：" + node.path("message").asText(""));
            }
        }
        throw new IllegalStateException("DashScope 任务超时（" + timeoutMs + "ms）");
    }

    /** 从生成端点推导任务查询端点：`.../services/aigc/text2image/image-synthesis` -> `.../api/v1/tasks`。 */
    private String tasksEndpoint(ProviderConfig provider) {
        String endpoint = provider.endpoint();
        int services = endpoint.indexOf("/services/");
        String base = services > 0 ? endpoint.substring(0, services) : "https://dashscope.aliyuncs.com/api/v1";
        return base + "/api/v1/tasks";
    }

    private String abbreviate(String text) {
        return text == null ? "" : (text.length() > 300 ? text.substring(0, 300) : text);
    }
}
