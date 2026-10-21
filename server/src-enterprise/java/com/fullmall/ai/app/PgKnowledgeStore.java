package com.fullmall.ai.app;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 生产知识库：关键词候选 + 向量余弦重排（混合召回）。
 *
 * <p>为什么不是纯向量：没有 pgvector 扩展时全表算余弦代价高；先用关键词把候选缩到几百条，
 * 再在应用内做余弦重排，正确性一致、成本可控。数据量上去后按文档切到 pgvector ANN。
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.ai.mode", havingValue = "prod")
public class PgKnowledgeStore implements KnowledgeStore {

    private static final int CANDIDATE_LIMIT = 200;

    private final JdbcTemplate jdbc;

    public PgKnowledgeStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void replaceSource(String sourceType, String sourceId, List<KnowledgeChunk> chunks) {
        jdbc.update("DELETE FROM knowledge_chunk WHERE source_type = ? AND source_id = ?",
                sourceType, sourceId);
        for (KnowledgeChunk chunk : chunks) {
            jdbc.update("""
                    INSERT INTO knowledge_chunk (id, source_type, source_id, title, content, version, embedding)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, chunk.id(), chunk.sourceType(), chunk.sourceId(),
                    chunk.title(), chunk.content(), chunk.version(), null);
        }
    }

    /** 写入时带向量（生产路径）。embedding 为 null 表示该块只能关键词命中。 */
    public void replaceSourceWithEmbeddings(String sourceType, String sourceId,
                                            List<KnowledgeChunk> chunks, List<float[]> embeddings) {
        if (chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("块数与向量数不一致："
                    + chunks.size() + " vs " + embeddings.size());
        }
        jdbc.update("DELETE FROM knowledge_chunk WHERE source_type = ? AND source_id = ?",
                sourceType, sourceId);
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = chunks.get(i);
            jdbc.update("""
                    INSERT INTO knowledge_chunk (id, source_type, source_id, title, content, version, embedding)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, chunk.id(), chunk.sourceType(), chunk.sourceId(),
                    chunk.title(), chunk.content(), chunk.version(),
                    embeddings.get(i) == null ? null : VectorMath.toBytes(embeddings.get(i)));
        }
    }

    @Override
    public List<KnowledgeChunk> search(String query, int topK) {
        return search(query, null, topK);
    }

    /**
     * @param queryVector 查询向量；为 null 时退化为纯关键词排序（演示/未配置嵌入时的显式行为）
     */
    public List<KnowledgeChunk> search(String query, float[] queryVector, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        List<String> terms = new ArrayList<>();
        for (String raw : query.toLowerCase().split("[\\s,，。？?！!；;：:]+")) {
            if (raw.length() >= 2) {
                terms.add("%" + raw + "%");
            }
        }
        if (terms.isEmpty()) {
            return List.of();
        }
        StringBuilder where = new StringBuilder();
        for (int i = 0; i < terms.size(); i++) {
            where.append(i == 0 ? "" : " OR ").append("lower(title || ' ' || content) LIKE ?");
        }
        List<Object> args = new ArrayList<>(terms);
        args.add(CANDIDATE_LIMIT);

        record Row(KnowledgeChunk chunk, byte[] embedding) {
        }
        List<Row> rows = jdbc.query("""
                SELECT id, source_type, source_id, title, content, version, embedding
                  FROM knowledge_chunk
                 WHERE %s
                 ORDER BY updated_at DESC
                 LIMIT ?
                """.formatted(where), (ResultSet rs, int rowNum) -> new Row(
                new KnowledgeChunk(rs.getObject("id", UUID.class), rs.getString("source_type"),
                        rs.getString("source_id"), rs.getString("title"),
                        rs.getString("content"), rs.getString("version")),
                rs.getBytes("embedding")), args.toArray());

        if (queryVector == null) {
            return rows.stream().limit(topK).map(Row::chunk).toList();
        }
        record Scored(KnowledgeChunk chunk, double score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (Row row : rows) {
            double score = VectorMath.cosine(queryVector, VectorMath.fromBytes(row.embedding()));
            if (score >= 0) {   // 无向量或维度不符的块不参与向量排序
                scored.add(new Scored(row.chunk(), score));
            }
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        if (scored.isEmpty()) {
            return rows.stream().limit(topK).map(Row::chunk).toList();
        }
        return scored.stream().limit(topK).map(Scored::chunk).toList();
    }

    @Override
    public int size() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_chunk", Integer.class);
        return count == null ? 0 : count;
    }
}
