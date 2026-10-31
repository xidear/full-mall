package com.fullmall.ai.answer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 关键词检索（无需 embedding 供应商）。
 *
 * <p>为什么需要它：语义检索要求 OpenAI 兼容的 embedding 服务，本机/内网经常没有；
 * 没有向量服务时，AI 客服不该整条链路 503。检索方式由 {@code app.ai.ask.retrieval}
 * **显式选择**（SEMANTIC / KEYWORD），不是"失败后偷偷降级"——运维在 AI 状态页能看到当前模式。
 *
 * <p>打分方式：中文按字符二元组（bigram）重合率，拉丁词按空格切分；
 * 完全包含问题的条目直接给 0.95。数据量在千级，Java 侧算即可，避免引入分词器与 pg_trgm 扩展。
 */
@Service
public class KeywordRetriever {

    private static final int FAQ_LIMIT = 1000;
    private static final int CHUNK_LIMIT = 2000;

    private final JdbcTemplate jdbc;

    public KeywordRetriever(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Hit(String sourceId, String title, String content, double score) {
    }

    /**
     * @param kind "FAQ" 只查问答；null 表示 FAQ + 导入文档知识块
     */
    public List<Hit> search(String query, String kind, int limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("检索问题不能为空");
        }
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        List<Hit> candidates = new ArrayList<>();
        if (kind == null || "FAQ".equalsIgnoreCase(kind)) {
            candidates.addAll(jdbc.query("""
                    SELECT id::text AS source_id, question AS title,
                           question || E'\\n' || answer || E'\\n' || keywords AS content
                      FROM faq_entry
                     WHERE status = 'ACTIVE'
                     ORDER BY created_at DESC LIMIT ?
                    """, (rs, rowNum) -> new Hit(rs.getString("source_id"), rs.getString("title"),
                    rs.getString("content"), 0.0), FAQ_LIMIT));
        }
        if (kind == null || "KNOWLEDGE".equalsIgnoreCase(kind)) {
            candidates.addAll(jdbc.query("""
                    SELECT source_id, title, title || E'\\n' || content AS content
                      FROM knowledge_chunk
                     ORDER BY updated_at DESC LIMIT ?
                    """, (rs, rowNum) -> new Hit(rs.getString("source_id"), rs.getString("title"),
                    rs.getString("content"), 0.0), CHUNK_LIMIT));
        }
        List<Hit> scored = new ArrayList<>();
        for (Hit candidate : candidates) {
            double score = score(normalizedQuery, candidate.content());
            if (score > 0) {
                scored.add(new Hit(candidate.sourceId(), candidate.title(), candidate.content(),
                        Math.round(score * 1000) / 1000.0));
            }
        }
        scored.sort(Comparator.comparingDouble(Hit::score).reversed()
                .thenComparing(Hit::title, Comparator.nullsLast(Comparator.naturalOrder())));
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return scored.subList(0, Math.min(safeLimit, scored.size()));
    }

    private static boolean containment(String query, String text) {
        return !query.isEmpty() && text.contains(query);
    }

    /** 对外可测的打分：完全包含 0.95，否则按 bigram 重合率（0..1）。 */
    static double score(String normalizedQuery, String rawText) {
        String text = normalize(rawText);
        if (containment(normalizedQuery, text)) {
            return 0.95;
        }
        return overlap(grams(normalizedQuery), grams(text));
    }

    /** 二元组重合率：query 的 bigram 有多少出现在候选文本里（0..1）。 */
    private static double overlap(Set<String> queryGrams, Set<String> textGrams) {
        if (queryGrams.isEmpty()) {
            return 0;
        }
        int hit = 0;
        for (String gram : queryGrams) {
            if (textGrams.contains(gram)) {
                hit++;
            }
        }
        return (double) hit / queryGrams.size();
    }

    private static Set<String> grams(String text) {
        Set<String> grams = new LinkedHashSet<>();
        if (text.isEmpty()) {
            return grams;
        }
        if (text.length() == 1) {
            grams.add(text);
            return grams;
        }
        for (int i = 0; i < text.length() - 1; i++) {
            grams.add(text.substring(i, i + 2));
        }
        return grams;
    }

    /** 去空白与标点，统一小写（全角/半角差异由数据库中文本本身决定，不做额外归一化）。 */
    static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{P}\\p{S}]+", "");
    }
}