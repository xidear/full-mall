package com.fullmall.ai.answer;

import com.fullmall.ai.knowledge.SemanticIndexService;
import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 客服问答的最小闭环：**只依据知识库检索结果作答，检索不到就拒答**。
 *
 * <p>为什么不做"模型自由发挥"：没有知识依据的回答就是幻觉，客服场景下代价是错误承诺。
 * 因此这里的判定顺序是硬的：
 * <ol>
 *   <li>没配对话模型 → 503（安装未完成，不是"没答案"）；</li>
 *   <li>语义检索未开启 → 503（明确告诉运维开哪个开关，不偷偷退回关键词检索）；</li>
 *   <li>检索为空或最高分低于阈值 → 200 + {@code fallback=true}，由前端转人工；</li>
 *   <li>只有命中片段才拼答案，并且必须带引用（sourceId + 分数）。</li>
 * </ol>
 *
 * <p>当前答案由命中片段直接拼装（可核对、不会编），后续接上真实模型时，
 * 约束不变：只允许基于这些片段生成，且必须回传同样的引用。
 */
@Service
public class AiAnswerService {

    public record Citation(String sourceId, double score, String snippet) {
    }

    public record Answer(String answer, boolean fallback, String reason,
                         List<Citation> citations, double topScore) {
    }

    private final JdbcTemplate jdbc;
    private final com.fullmall.ai.media.AiMediaService media;
    private final SemanticIndexService index;
    private final AuditEventService auditEvents;
    private final double minScore;
    private final int topK;

    public AiAnswerService(JdbcTemplate jdbc, com.fullmall.ai.media.AiMediaService media,
                           SemanticIndexService index,
                           AuditEventService auditEvents,
                           @Value("${app.ai.ask.min-score:0.72}") double minScore,
                           @Value("${app.ai.ask.top-k:5}") int topK,
                           @Value("${app.ai.ask.banned-words:}")
                           String bannedWords) {
        this.jdbc = jdbc;
        this.media = media;
        this.index = index;
        this.auditEvents = auditEvents;
        this.minScore = minScore;
        this.topK = topK;
        this.bannedWords = bannedWords == null || bannedWords.isBlank()
                ? DEFAULT_BANNED
                : java.util.Arrays.stream(bannedWords.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /**
     * 默认禁词（辱骂/越界承诺）。可用 app.ai.ask.banned-words 覆盖（逗号分隔）。
     *
     * <p>命中后的行为是固定的：不回答、给固定话术、记审计并建议转人工——
     * 不指望模型"自己判断要不要骂回去"。
     */
    private static final List<String> DEFAULT_BANNED = List.of(
            "傻逼", "白痴", "去死", "滚蛋", "你妈");
    private final List<String> bannedWords;

    public Answer ask(String question, String actor) {
        if (question == null || question.isBlank()) {
            throw ApiException.badRequest("AI_QUESTION_REQUIRED", "问题不能为空");
        }
        String bannedHit = firstBanned(question);
        if (bannedHit != null) {
            auditEvents.record(new AuditEventService.AuditEvent("AI_QUESTION_BLOCKED", actor,
                    "ai_ask", null, "hit=" + bannedHit + " q=" + shorten(question), null));
            return new Answer(null, true, "问题包含不当内容，已转人工处理", List.of(), 0.0);
        }

        String provider = enabledTextProvider();
        if (provider == null) {
            throw ApiException.serviceUnavailable("AI_PROVIDER_NOT_CONFIGURED",
                    "未配置可用的对话模型：请到「第三方服务 → AI 模型」启用一个文本供应商");
        }
        if (!index.enabled()) {
            throw ApiException.serviceUnavailable("AI_SEMANTIC_DISABLED",
                    "知识库检索未开启：设置 APP_AI_SEMANTIC_ENABLED=true 并建立索引后再试");
        }

        List<Map<String, Object>> hits;
        try {
            hits = index.search(question.trim(), "FAQ", topK, provider);
        } catch (ApiException failure) {
            // 索引为空是"没有知识"→ 拒答；embedding 缺失是"没装好"→ 503，不能混成一个错误
            if ("SEMANTIC_INDEX_EMPTY".equals(failure.code())) {
                return refuse(actor, question, 0.0, "知识库为空");
            }
            if ("AI_EMBEDDING_UNAVAILABLE".equals(failure.code())) {
                throw ApiException.serviceUnavailable("AI_EMBEDDING_NOT_CONFIGURED",
                        "未配置可用的向量模型（embedding）：请到「第三方服务 → AI 模型」启用一个 protocol=openai-embedding 的供应商");
            }
            throw failure;
        }

        List<Citation> citations = new ArrayList<>();
        for (Map<String, Object> hit : hits) {
            double score = ((Number) hit.get("score")).doubleValue();
            if (score < minScore) {
                continue;
            }
            citations.add(new Citation(String.valueOf(hit.get("sourceId")), score,
                    shorten(String.valueOf(hit.get("content")))));
        }

        if (citations.isEmpty()) {
            double best = hits.isEmpty() ? 0.0 : ((Number) hits.get(0).get("score")).doubleValue();
            return refuse(actor, question, best, "检索结果低于可信阈值");
        }

        StringBuilder answer = new StringBuilder("根据知识库：\n");
        for (int i = 0; i < citations.size(); i++) {
            answer.append(i + 1).append(". ").append(citations.get(i).snippet()).append('\n');
        }
        answer.append("（以上内容来自知识库条目，可在引用中核对；如需进一步判断请转人工。）");

        String answerBanned = firstBanned(answer.toString());
        if (answerBanned != null) {
            auditEvents.record(new AuditEventService.AuditEvent("AI_ANSWER_BLOCKED", actor,
                    "ai_ask", null, "hit=" + answerBanned + " q=" + shorten(question), null));
            return new Answer(null, true, "回答命中内容约束，已转人工处理", List.of(), citations.get(0).score());
        }

        auditEvents.record(new AuditEventService.AuditEvent("AI_KNOWLEDGE_ANSWERED", actor,
                "ai_ask", null, "citations=" + citations.size()
                + " topScore=" + citations.get(0).score(), null));
        return new Answer(answer.toString(), false, null, citations, citations.get(0).score());
    }

    private Answer refuse(String actor, String question, double topScore, String reason) {
        auditEvents.record(new AuditEventService.AuditEvent("AI_KNOWLEDGE_REFUSED", actor,
                "ai_ask", null, "reason=" + reason + " topScore=" + topScore
                + " q=" + shorten(question), null));
        return new Answer(null, true, reason, List.of(), topScore);
    }

    private String enabledTextProvider() {
        List<String> providers = jdbc.queryForList(
                "SELECT code FROM ai_provider WHERE enabled AND kind = 'TEXT' LIMIT 1", String.class);
        return providers.isEmpty() ? null : providers.get(0);
    }

    /**
     * 未命中/被拦截的问题清单（运营据此补知识库）。
     *
     * <p>数据来自审计（AI_KNOWLEDGE_REFUSED / AI_QUESTION_BLOCKED），
     * 不额外建表：审计本来就是这些事实的唯一来源，多一张表只会出现两套事实。
     */
    public List<Map<String, Object>> recentUnanswered(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return jdbc.query("""
                SELECT created_at, event_type, actor, detail FROM audit_event
                 WHERE event_type IN ('AI_KNOWLEDGE_REFUSED', 'AI_QUESTION_BLOCKED')
                 ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> {
            String detail = rs.getString("detail");
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("at", rs.getTimestamp("created_at").toInstant().toString());
            row.put("type", rs.getString("event_type"));
            row.put("actor", rs.getString("actor"));
            row.put("detail", detail);
            int idx = detail == null ? -1 : detail.indexOf("q=");
            row.put("question", idx >= 0 ? detail.substring(idx + 2) : null);
            return row;
        }, safeLimit);
    }

    private String firstBanned(String text) {
        if (text == null) {
            return null;
        }
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        for (String word : bannedWords) {
            if (!word.isEmpty() && lower.contains(word.toLowerCase(java.util.Locale.ROOT))) {
                return word;
            }
        }
        return null;
    }

    private static String shorten(String content) {
        String trimmed = content == null ? "" : content.trim();
        return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 200) + "…";
    }
}
