package com.fullmall.ai.knowledge;

import com.fullmall.ai.media.MediaKind;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.json.Json;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 语义检索（L9）：用自有 embedding 供应商建索引，查询时算余弦相似度。
 *
 * <p>为什么 Java 侧算：数据量（FAQ/知识条目）在千级，避免为 pgvector 引入扩展与运维成本；
 * 超过 10 万条应改用 pgvector / OpenSearch k-NN（已在 compose 里）。
 *
 * <p>开关：`app.ai.semantic.enabled`，默认关闭；**关闭时不会偷偷退回关键词**，
 * 调用方需显式选择关键词接口。
 */
@Service
public class SemanticIndexService {

    private final JdbcTemplate jdbc;
    private final com.fullmall.ai.media.AiMediaService media;
    private final boolean enabled;

    public SemanticIndexService(JdbcTemplate jdbc, com.fullmall.ai.media.AiMediaService media,
                                @Value("${app.ai.semantic.enabled:false}") boolean enabled) {
        this.jdbc = jdbc;
        this.media = media;
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    @Transactional
    public int index(String kind, String sourceId, String content, String providerCode) {
        requireEnabled();
        if (content == null || content.isBlank()) {
            throw ApiException.badRequest("SEMANTIC_CONTENT_REQUIRED", "索引内容不能为空");
        }
        double[] vector = embed(content, providerCode);
        jdbc.update("""
                INSERT INTO knowledge_embedding (id, kind, source_id, content, embedding, provider, model)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (kind, source_id) DO UPDATE
                   SET content = EXCLUDED.content, embedding = EXCLUDED.embedding,
                       provider = EXCLUDED.provider, model = EXCLUDED.model, created_at = now()
                """, UUID.randomUUID(), kind, sourceId, content, toSqlArray(vector),
                providerCode == null ? "default" : providerCode, "embedding");
        return 1;
    }

    /** 批量重建某类索引（例如把 FAQ 全量重建）。 */
    @Transactional
    public int reindexKind(String kind, String providerCode) {
        requireEnabled();
        int updated = 0;
        if ("FAQ".equalsIgnoreCase(kind)) {
            var rows = jdbc.query("""
                    SELECT id, question, answer FROM faq_entry WHERE status = 'ACTIVE'
                    """, (rs, rowNum) -> new Object[]{rs.getObject("id", UUID.class),
                    rs.getString("question"), rs.getString("answer")});
            for (Object[] row : rows) {
                index("FAQ", row[0].toString(), row[1] + "\n" + row[2], providerCode);
                updated++;
            }
        }
        if (updated == 0) {
            throw ApiException.badRequest("SEMANTIC_NOTHING_TO_INDEX", "没有可索引的数据（kind=" + kind + "）");
        }
        return updated;
    }

    public List<Map<String, Object>> search(String query, String kind, int limit, String providerCode) {
        requireEnabled();
        if (query == null || query.isBlank()) {
            throw ApiException.badRequest("SEMANTIC_QUERY_REQUIRED", "查询内容不能为空");
        }
        double[] queryVector = embed(query, providerCode);
        int topK = Math.min(Math.max(limit, 1), 50);
        List<Map<String, Object>> candidates = jdbc.query("""
                SELECT source_id, content, embedding FROM knowledge_embedding
                 WHERE (?::text IS NULL OR kind = ?::text)
                 ORDER BY created_at DESC LIMIT 2000
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sourceId", rs.getString("source_id"));
            row.put("content", rs.getString("content"));
            row.put("vector", toDoubleArray(rs.getArray("embedding")));
            return row;
        }, kind == null ? null : kind, kind == null ? null : kind);
        if (candidates.isEmpty()) {
            throw ApiException.notFound("SEMANTIC_INDEX_EMPTY", "索引为空，请先重建索引");
        }
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> candidate : candidates) {
            double score = cosine(queryVector, (double[]) candidate.get("vector"));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sourceId", candidate.get("sourceId"));
            row.put("content", candidate.get("content"));
            row.put("score", Math.round(score * 1000) / 1000.0);
            scored.add(row);
        }
        scored.sort((a, b) -> Double.compare((double) b.get("score"), (double) a.get("score")));
        return scored.subList(0, Math.min(topK, scored.size()));
    }

    private double[] embed(String text, String providerCode) {
        String json = media.embedding(text, providerCode);
        var node = Json.read(json);
        if (!node.isArray() || node.isEmpty()) {
            throw new IllegalStateException("embedding 返回格式异常");
        }
        double[] vector = new double[node.size()];
        for (int i = 0; i < node.size(); i++) {
            vector[i] = node.get(i).asDouble();
        }
        return vector;
    }

    private double cosine(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalStateException("向量维度不一致：" + a.length + " vs " + b.length);
        }
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

    private Array toSqlArray(double[] vector) {
        try {
            return jdbc.getDataSource().getConnection().createArrayOf("float8",
                    java.util.Arrays.stream(vector).boxed().toArray(Double[]::new));
        } catch (Exception error) {
            throw new IllegalStateException("向量转 SQL 数组失败", error);
        }
    }

    private double[] toDoubleArray(Array array) {
        try {
            Double[] values = (Double[]) array.getArray();
            double[] result = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                result[i] = values[i];
            }
            return result;
        } catch (Exception error) {
            throw new IllegalStateException("读取向量失败", error);
        }
    }

    private void requireEnabled() {
        if (!enabled) {
            throw ApiException.conflict("SEMANTIC_DISABLED",
                    "语义检索未启用（app.ai.semantic.enabled=false）");
        }
    }
}
