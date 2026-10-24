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
 * 阿里百炼多模态图像编辑（qwen-image-edit）：`messages[].content` 里放**多张** `{image}` + 文本。
 *
 * <p>官方示例形态：
 * <pre>
 * {"model":"qwen-image-edit","messages":[{"role":"user","content":[
 *    {"image":"https://.../front.png"},{"image":"https://.../side.png"},
 *    {"text":"把两张参考图合成一张白底主图"}]}]}
 * </pre>
 *
 * <p>返回可能是 `output.choices[0].message.content[]` 里的 image，也可能是 `output.results[0].url`，
 * 两种都兼容；都取不到就报错，不回退成空图。
 */
public class DashScopeMultimodalImageAdapter implements MediaGenerationAdapter {

    private final java.net.http.HttpClient http = com.fullmall.platform.http.HttpClients.shared();

    @Override
    public String protocol() {
        return "dashscope-multimodal";
    }

    @Override
    public GeneratedMedia generate(ProviderConfig provider, String prompt, String negative,
                                   Map<String, Object> params) {
        List<Map<String, Object>> content = new ArrayList<>();
        for (String url : referenceUrls(params)) {
            content.add(Map.of("image", url));
        }
        String text = negative == null || negative.isBlank() ? prompt
                : prompt + "\n（避免：" + negative + "）";
        content.add(Map.of("text", text));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", provider.model());
        body.put("messages", List.of(Map.of("role", "user", "content", content)));

        String endpoint = provider.endpoint().endsWith("/")
                ? provider.endpoint().substring(0, provider.endpoint().length() - 1)
                : provider.endpoint();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(180))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + provider.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("qwen-image-edit 失败 HTTP " + response.statusCode()
                        + "：" + abbreviate(response.body()));
            }
            var root = Json.read(response.body());
            String url = firstImageUrl(root.path("output"));
            if (url.isBlank()) {
                throw new IllegalStateException("qwen-image-edit 返回中没有图片 URL");
            }
            return GeneratedMedia.url(url, "image/png");
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("qwen-image-edit 请求失败：" + error.getMessage(), error);
        }
    }

    private String firstImageUrl(com.fasterxml.jackson.databind.JsonNode output) {
        var content = output.path("choices").path(0).path("message").path("content");
        if (content.isArray()) {
            for (var node : content) {
                if (node.has("image")) {
                    return node.path("image").asText("");
                }
            }
        }
        var results = output.path("results");
        if (results.isArray() && !results.isEmpty()) {
            return results.get(0).path("url").asText("");
        }
        return "";
    }

    private List<String> referenceUrls(Map<String, Object> params) {
        List<String> urls = new ArrayList<>();
        if (params == null || !(params.get("images") instanceof List<?> list)) {
            return urls;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map && map.get("url") != null) {
                urls.add(String.valueOf(map.get("url")));
            }
        }
        return urls;
    }

    private String abbreviate(String text) {
        return text == null ? "" : (text.length() > 300 ? text.substring(0, 300) : text);
    }
}
