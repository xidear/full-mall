package com.fullmall.ai.media.adapter;

import com.fullmall.ai.media.ProviderConfig;
import com.fullmall.platform.json.Json;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenAI 兼容的图片生成（`POST {endpoint}/images/generations`）。
 *
 * <p>可对接 OpenAI、多数国产兼容网关、自建 SD 网关。
 * 返回 `b64_json` 优先（直接拿字节），否则用 `url` 回源下载。
 */
public class OpenAiImagesAdapter implements MediaGenerationAdapter {

    private final java.net.http.HttpClient http =
            com.fullmall.platform.http.HttpClients.shared();

    @Override
    public String protocol() {
        return "openai-images";
    }

    @Override
    public GeneratedMedia generate(ProviderConfig provider, String prompt, String negative,
                                   Map<String, Object> params) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", provider.model());
        body.put("prompt", negative == null || negative.isBlank()
                ? prompt : prompt + "\n（避免：" + negative + "）");
        body.put("n", 1);
        if (params != null) {
            if (params.get("referenceImageUrls") instanceof java.util.List<?> refs && !refs.isEmpty()) {
                // 多数兼容网关接受 image 字段（URL）；不支持的会被服务端忽略或明确报错
                body.put("image", refs.get(0));
            }
            if (params.get("size") != null) {
                body.put("size", params.get("size"));
            }
            if (params.get("quality") != null) {
                body.put("quality", params.get("quality"));
            }
            if (params.get("response_format") != null) {
                body.put("response_format", params.get("response_format"));
            }
        }
        String base = provider.endpoint().endsWith("/")
                ? provider.endpoint().substring(0, provider.endpoint().length() - 1)
                : provider.endpoint();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/images/generations"))
                .timeout(Duration.ofSeconds(180))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + provider.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(provider.code() + " 生图失败 HTTP "
                        + response.statusCode() + "：" + abbreviate(response.body()));
            }
            var data = Json.read(response.body()).path("data");
            if (!data.isArray() || data.isEmpty()) {
                throw new IllegalStateException(provider.code() + " 生图返回缺少 data");
            }
            String b64 = data.get(0).path("b64_json").asText("");
            if (!b64.isBlank()) {
                return GeneratedMedia.bytes(Base64.getDecoder().decode(b64), "image/png");
            }
            String url = data.get(0).path("url").asText("");
            if (url.isBlank()) {
                throw new IllegalStateException(provider.code() + " 生图返回既无 b64_json 也无 url");
            }
            return GeneratedMedia.url(url, "image/png");
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("生图请求失败：" + error.getMessage(), error);
        }
    }

    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 300 ? text.substring(0, 300) : text;
    }
}
