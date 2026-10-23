package com.fullmall.support.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FAQ 知识库（L5）：检索 + **未命中记录** + 候选问题清单。
 *
 * <p>自学习闭环：用户问 → 没命中就记一条 miss → 后台看候选（按重复次数排序）→ 一键采纳成 FAQ。
 * 不做"模型自动写答案"：答案必须人工确认，避免把错答案喂回用户。
 */
@Service
public class FaqService {

    private final JdbcTemplate jdbc;

    public FaqService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Hit(UUID id, String question, String answer) {
    }

    /** 检索：命中返回答案；未命中记录 miss 并返回 null。 */
    @Transactional
    public Hit search(String question, UUID memberId) {
        if (question == null || question.isBlank()) {
            throw ApiException.badRequest("FAQ_QUESTION_REQUIRED", "问题不能为空");
        }
        String trimmed = question.trim();
        if (trimmed.length() > 300) {
            trimmed = trimmed.substring(0, 300);
        }
        List<Hit> hits = jdbc.query("""
                SELECT id, question, answer FROM faq_entry
                 WHERE status = 'ACTIVE'
                   AND (lower(question) LIKE '%' || lower(?) || '%'
                        OR lower(keywords) LIKE '%' || lower(?) || '%')
                 ORDER BY length(question) LIMIT 1
                """, (rs, rowNum) -> new Hit(rs.getObject("id", UUID.class),
                rs.getString("question"), rs.getString("answer")), trimmed, trimmed);
        if (!hits.isEmpty()) {
            return hits.get(0);
        }
        jdbc.update("INSERT INTO faq_miss (id, question, member_id) VALUES (?, ?, ?)",
                UUID.randomUUID(), trimmed, memberId);
        return null;
    }

    /** 候选：近 N 天未命中且库里没有对应 FAQ，按出现次数降序。 */
    public List<Map<String, Object>> candidates(int days, int minCount) {
        int window = Math.min(Math.max(days, 1), 90);
        int threshold = Math.max(minCount, 1);
        return jdbc.query("""
                SELECT lower(question) AS normalized, MIN(question) AS sample,
                       COUNT(*) AS hits, MAX(created_at) AS last_seen
                  FROM faq_miss m
                 WHERE m.created_at > now() - (? || ' days')::interval
                   AND NOT EXISTS (
                        SELECT 1 FROM faq_entry e
                         WHERE e.status = 'ACTIVE'
                           AND lower(e.question) LIKE '%' || lower(m.question) || '%')
                 GROUP BY lower(question)
                HAVING COUNT(*) >= ?
                 ORDER BY hits DESC, last_seen DESC
                 LIMIT 50
                """, (rs, rowNum) -> Map.of(
                "question", rs.getString("sample"),
                "hits", rs.getLong("hits"),
                "lastSeen", rs.getTimestamp("last_seen").toInstant().toString()),
                String.valueOf(window), threshold);
    }

    @Transactional
    public Hit adopt(String question, String answer) {
        if (question == null || question.isBlank() || answer == null || answer.isBlank()) {
            throw ApiException.badRequest("FAQ_FIELDS_REQUIRED", "问题与答案都不能为空");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO faq_entry (id, question, answer) VALUES (?, ?, ?)
                ON CONFLICT (question) DO UPDATE SET answer = EXCLUDED.answer, status = 'ACTIVE'
                """, id, question.trim(), answer.trim());
        var rows = jdbc.query("""
                SELECT id, question, answer FROM faq_entry WHERE question = ?
                """, (rs, rowNum) -> new Hit(rs.getObject("id", UUID.class),
                rs.getString("question"), rs.getString("answer")), question.trim());
        return rows.get(0);
    }

    public List<Hit> list(String keyword) {
        return jdbc.query("""
                SELECT id, question, answer FROM faq_entry
                 WHERE status = 'ACTIVE'
                   AND (? IS NULL OR lower(question) LIKE '%' || lower(?) || '%')
                 ORDER BY created_at DESC LIMIT 100
                """, (rs, rowNum) -> new Hit(rs.getObject("id", UUID.class),
                rs.getString("question"), rs.getString("answer")),
                keyword == null || keyword.isBlank() ? null : keyword,
                keyword == null || keyword.isBlank() ? null : keyword);
    }
}
