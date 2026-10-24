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
        java.util.List<String> refs = referenceUrls(params);
        if (!refs.isEmpty()) {
            // gpt-image-1 官方多图编辑：POST /images/edits，multipart 里 image[] 可多份
            return edit(base, provider, prompt, refs, params);
        }
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

    private java.util.List<String> referenceUrls(java.util.Map<String, Object> params) {
        java.util.List<String> urls = new java.util.ArrayList<>();
        if (params == null || !(params.get("referenceImageUrls") instanceof java.util.List<?> list)) {
            return urls;
        }
        for (Object item : list) {
            if (item != null) {
                urls.add(String.valueOf(item));
            }
        }
        return urls;
    }

    /** multipart/form-data 手工拼装（避免为一次请求引入额外依赖）。 */
    private GeneratedMedia edit(String base, ProviderConfig provider, String prompt,
                                java.util.List<String> imageUrls, java.util.Map<String, Object> params) {
        String boundary = "----fullmall" + java.util.UUID.randomUUID().toString().replace("-", "");
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            writeField(out, boundary, "model", provider.model());
            writeField(out, boundary, "prompt", prompt);
            writeField(out, boundary, "n", "1");
            if (params != null && params.get("size") != null) {
                writeField(out, boundary, "size", String.valueOf(params.get("size")));
            }
            int index = 0;
            for (String url : imageUrls) {
                byte[] image = download(url);
                index++;
                out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                out.write(("Content-Disposition: form-data; name=\"image[]\"; filename=\"ref"
                        + index + ".png\"\r\n").getBytes(StandardCharsets.UTF_8));
                out.write("Content-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                out.write(image);
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/images/edits"))
                    .timeout(Duration.ofSeconds(180))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Authorization", "Bearer " + provider.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(provider.code() + " 多图编辑失败 HTTP "
                        + response.statusCode() + "：" + abbreviate(response.body()));
            }
            var data = Json.read(response.body()).path("data");
            if (!data.isArray() || data.isEmpty()) {
                throw new IllegalStateException(provider.code() + " 多图编辑返回缺少 data");
            }
            String b64 = data.get(0).path("b64_json").asText("");
            if (!b64.isBlank()) {
                return GeneratedMedia.bytes(Base64.getDecoder().decode(b64), "image/png");
            }
            String url = data.get(0).path("url").asText("");
            if (url.isBlank()) {
                throw new IllegalStateException(provider.code() + " 多图编辑返回缺少图片");
            }
            return GeneratedMedia.url(url, "image/png");
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("多图编辑请求失败：" + error.getMessage(), error);
        }
    }

    private void writeField(java.io.ByteArrayOutputStream out, String boundary, String name,
                            String value) throws java.io.IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private byte[] download(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("参考图下载失败 HTTP " + response.statusCode() + "：" + url);
        }
        return response.body();
    }
}
