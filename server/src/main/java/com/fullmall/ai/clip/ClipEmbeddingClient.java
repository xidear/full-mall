package com.fullmall.ai.clip;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.json.Json;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 多模态（CLIP）embedding 客户端：文字与图片映射到**同一个向量空间**，
 * 因此可以用一句自然语言直接搜图片内容（"白底马克杯"）。
 *
 * <p>与文本 embedding（{@link com.fullmall.ai.knowledge.SemanticIndexService}）的区别：
 * 那边检索的是"描述文字"的语义，这边检索的是"图像像素"的语义——图片没有描述也能被搜到。
 *
 * <p>供应商由 ai_provider 表配置（protocol=clip-http），**不写死本地服务**：
 * 客户可以指向远端多模态服务；没配置时接口返回 409，不静默降级。
 */
@Service
public class ClipEmbeddingClient {

    private final JdbcTemplate jdbc;
    private final java.net.http.HttpClient http = com.fullmall.platform.http.HttpClients.shared();

    public ClipEmbeddingClient(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 是否已配置可用的 CLIP 供应商（前端据此决定是否显示"按图片内容搜索"）。 */
    public boolean configured() {
        return !providers(null).isEmpty();
    }

    /** 文本 → 图像空间向量（用于"用文字搜图"）。 */
    public double[] embedText(String text, String providerCode) {
        if (text == null || text.isBlank()) {
            throw ApiException.badRequest("CLIP_TEXT_REQUIRED", "搜索词不能为空");
        }
        return post("/embed/text", Json.write(java.util.Map.of("text", text)), providerCode);
    }

    /** 图片字节 → 图像向量（建索引用）。 */
    public double[] embedImage(byte[] image, String providerCode) {
        if (image == null || image.length == 0) {
            throw ApiException.badRequest("CLIP_IMAGE_REQUIRED", "图片内容为空");
        }
        return post("/embed/image",
                Json.write(java.util.Map.of("base64", Base64.getEncoder().encodeToString(image))),
                providerCode);
    }

    private double[] post(String path, String body, String providerCode) {
        var provider = providers(providerCode).stream().findFirst().orElseThrow(() ->
                ApiException.conflict("CLIP_UNAVAILABLE",
                        "未配置多模态向量供应商（ai_provider 里设置 protocol=clip-http）"));
        String endpoint = provider.endpoint().endsWith("/")
                ? provider.endpoint().substring(0, provider.endpoint().length() - 1)
                : provider.endpoint();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + path))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(provider.code() + " 向量化失败 HTTP "
                        + response.statusCode() + "：" + response.body());
            }
            var node = Json.read(response.body()).path("vector");
            if (!node.isArray() || node.isEmpty()) {
                throw new IllegalStateException(provider.code() + " 返回向量为空");
            }
            double[] vector = new double[node.size()];
            for (int i = 0; i < node.size(); i++) {
                vector[i] = node.get(i).asDouble();
            }
            return vector;
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("多模态向量请求失败：" + error.getMessage(), error);
        }
    }

    /** 供应商行：protocol=clip-http 且启用；指定 code 时只取它。 */
    public List<Row> providers(String code) {
        return jdbc.query("""
                SELECT id, code, name, endpoint, model
                  FROM ai_provider
                 WHERE enabled AND protocol = 'clip-http'
                   AND (?::text IS NULL OR code = ?::text)
                 ORDER BY priority, code
                """, (rs, rowNum) -> new Row(rs.getObject("id", UUID.class), rs.getString("code"),
                rs.getString("name"), rs.getString("endpoint"), rs.getString("model")), code, code);
    }

    public record Row(UUID id, String code, String name, String endpoint, String model) {
    }
}
