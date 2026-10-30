package com.fullmall.asset.app;

import com.fullmall.ai.clip.ClipEmbeddingClient;
import com.fullmall.platform.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 素材图像向量：建索引 + 用自然语言检索图片内容。
 *
 * <p>与 {@link AssetSearchService#semanticSearch} 的分工：
 * 那边比较的是"描述/标签的文字向量"，描述来自人工或 VLM，没描述就搜不到；
 * 这边比较的是图像本身的向量，**不依赖任何文字描述**。
 *
 * <p>为什么在 Java 里算余弦：素材量在千级，避免为 pgvector 增加扩展依赖；
 * 上万条后应换 pgvector / OpenSearch k-NN。
 */
@Service
public class AssetImageIndexService {

    private final JdbcTemplate jdbc;
    private final ClipEmbeddingClient clip;
    private final String localDirectory;
    private final String model;

    public AssetImageIndexService(JdbcTemplate jdbc, ClipEmbeddingClient clip,
                                  @Value("${app.asset.local.directory:./data/assets}") String localDirectory,
                                  @Value("${app.ai.clip.model:clip-vit-base-patch32}") String model) {
        this.jdbc = jdbc;
        this.clip = clip;
        this.localDirectory = localDirectory;
        this.model = model;
    }

    public boolean available() {
        return clip.configured();
    }

    /** 给单张素材建图像索引；非图片或文件缺失都明确失败。 */
    public double[] index(UUID assetId, String providerCode) {
        var rows = jdbc.query("SELECT url, kind FROM asset WHERE id = ? AND deleted_at IS NULL",
                (rs, rowNum) -> new String[]{rs.getString("url"), rs.getString("kind")}, assetId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("ASSET_NOT_FOUND", "素材不存在：" + assetId);
        }
        String url = rows.get(0)[0];
        String kind = rows.get(0)[1];
        if (!"IMAGE".equalsIgnoreCase(kind)) {
            throw ApiException.badRequest("ASSET_NOT_IMAGE", "只有图片可以建图像索引：" + kind);
        }
        byte[] bytes = readBytes(url);
        double[] vector = clip.embedImage(bytes, providerCode);
        upsert(assetId, vector, providerCode);
        return vector;
    }

    /** 批量重建（忽略单张失败会让结果失真，这里只在全部成功时返回数量）。 */
    public int reindexAll(String providerCode) {
        var ids = jdbc.queryForList("SELECT id FROM asset WHERE deleted_at IS NULL AND kind = 'IMAGE'",
                UUID.class);
        int done = 0;
        for (UUID id : ids) {
            index(id, providerCode);
            done++;
        }
        return done;
    }

    /**
     * 用一句话搜图片内容：查询词先过 CLIP 文本编码器，落到同一向量空间再比余弦。
     *
     * @return 命中素材与相似度，按相似度倒序（相似度只在同一模型下可比）
     */
    public List<Hit> search(String query, int limit, String providerCode) {
        if (query == null || query.isBlank()) {
            throw ApiException.badRequest("SEARCH_QUERY_REQUIRED", "搜索词不能为空");
        }
        double[] queryVector = clip.embedText(query, providerCode);
        List<Hit> hits = new ArrayList<>();
        jdbc.query("""
                SELECT e.asset_id, e.vector FROM asset_image_embedding e
                  JOIN asset a ON a.id = e.asset_id AND a.deleted_at IS NULL
                """, rs -> {
            UUID assetId = rs.getObject("asset_id", UUID.class);
            double[] vector = toDoubles((Double[]) rs.getArray("vector").getArray());
            if (vector.length == queryVector.length) {
                hits.add(new Hit(assetId, cosine(queryVector, vector)));
            }
        });
        hits.sort((a, b) -> Double.compare(b.score(), a.score()));
        return hits.subList(0, Math.min(limit, hits.size()));
    }

    public record Hit(UUID assetId, double score) {
    }

    private void upsert(UUID assetId, double[] vector, String providerCode) {
        UUID unused = UUID.randomUUID();
        jdbc.execute((java.sql.Connection connection) -> {
            try (java.sql.PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO asset_image_embedding (asset_id, vector, model, provider, updated_at)
                    VALUES (?, ?, ?, ?, now())
                    ON CONFLICT (asset_id) DO UPDATE
                       SET vector = EXCLUDED.vector, model = EXCLUDED.model,
                           provider = EXCLUDED.provider, updated_at = now()
                    """)) {
                // 用同一条连接建数组并 free()：历史上 toSqlArray() 泄漏连接池
                java.sql.Array array = connection.createArrayOf("float8",
                        java.util.Arrays.stream(vector).boxed().toArray(Double[]::new));
                try {
                    statement.setObject(1, assetId);
                    statement.setArray(2, array);
                    statement.setString(3, model);
                    statement.setString(4, providerCode);
                    statement.executeUpdate();
                } finally {
                    array.free();
                }
            }
            return unused;
        });
    }

    private byte[] readBytes(String url) {
        if (url == null || url.isBlank()) {
            throw ApiException.badRequest("ASSET_URL_REQUIRED", "素材缺少图片地址");
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            throw ApiException.badRequest("ASSET_REMOTE_NOT_SUPPORTED",
                    "暂不支持远端图片直接建索引，请先转存到本站存储：" + url);
        }
        String relative = url.startsWith("/assets/") ? url.substring("/assets/".length())
                : url.replaceFirst("^/", "");
        java.nio.file.Path path = java.nio.file.Path.of(localDirectory).resolve(relative).normalize();
        try {
            return java.nio.file.Files.readAllBytes(path);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("读取素材文件失败：" + path, error);
        }
    }

    private double[] toDoubles(Double[] values) {
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }
        return result;
    }

    private double cosine(double[] a, double[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
