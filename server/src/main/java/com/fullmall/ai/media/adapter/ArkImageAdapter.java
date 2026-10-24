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
 * 火山方舟 Seedream（图片）：`content[]` 里每张参考图 `role:"reference_image"`。
 *
 * <p>官方形态（多图参考）：
 * <pre>
 * {"model":"doubao-seedream-...","content":[
 *    {"type":"text","text":"..."},
 *    {"type":"image_url","image_url":{"url":"https://..."},"role":"reference_image"}, ...]}
 * </pre>
 */
public class ArkImageAdapter implements MediaGenerationAdapter {

    private final java.net.http.HttpClient http = com.fullmall.platform.http.HttpClients.shared();

    @Override
    public String protocol() {
        return "ark-image";
    }

    @Override
    public GeneratedMedia generate(ProviderConfig provider, String prompt, String negative,
                                   Map<String, Object> params) {
        List<Map<String, Object>> content = new ArrayList<>();
        String text = negative == null || negative.isBlank() ? prompt
                : prompt + "\n（避免：" + negative + "）";
        content.add(Map.of("type", "text", "text", text));
        for (String url : referenceUrls(params)) {
            Map<String, Object> image = new LinkedHashMap<>();
            image.put("type", "image_url");
            image.put("image_url", Map.of("url", url));
            image.put("role", "reference_image");
            content.add(image);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", provider.model());
        body.put("content", content);
        if (params != null && params.get("size") != null) {
            body.put("size", params.get("size"));
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trim(provider.endpoint()) + "/images/generations"))
                .timeout(Duration.ofSeconds(180))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + provider.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Ark 生图失败 HTTP " + response.statusCode()
                        + "：" + abbreviate(response.body()));
            }
            var data = Json.read(response.body()).path("data");
            if (!data.isArray() || data.isEmpty()) {
                throw new IllegalStateException("Ark 生图返回缺少 data");
            }
            String url = data.get(0).path("url").asText("");
            if (url.isBlank()) {
                throw new IllegalStateException("Ark 生图返回缺少 url");
            }
            return GeneratedMedia.url(url, "image/png");
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("Ark 生图请求失败：" + error.getMessage(), error);
        }
    }

    private List<String> referenceUrls(Map<String, Object> params) {
        List<String> urls = new ArrayList<>();
        if (params == null) {
            return urls;
        }
        Object images = params.get("images");
        if (images instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && map.get("url") != null) {
                    urls.add(String.valueOf(map.get("url")));
                }
            }
        }
        return urls;
    }

    private String trim(String endpoint) {
        return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    private String abbreviate(String text) {
        return text == null ? "" : (text.length() > 300 ? text.substring(0, 300) : text);
    }
}
