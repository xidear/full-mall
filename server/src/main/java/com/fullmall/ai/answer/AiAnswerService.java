package com.fullmall.ai.answer;

import com.fullmall.ai.knowledge.SemanticIndexService;
import com.fullmall.ai.media.AiMediaService;
import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AI 客服问答：**检索 → 真实对话模型 → 强制带引用**。
 *
 * <p>判定顺序是硬的，避免把"没装好"伪装成"没答案"：
 * <ol>
 *   <li>没配对话模型（protocol=openai-chat）→ 503；</li>
 *   <li>检索模式由 {@code app.ai.ask.retrieval} 显式选择：
 *       KEYWORD 不需要向量服务；SEMANTIC 未开启或缺 embedding → 503；</li>
 *   <li>检索为空或最高分低于阈值 → 200 + {@code fallback=true}，由前端转人工；</li>
 *   <li>只有命中片段才调用模型，system 明确"只能依据片段"；模型说无法回答 → 仍转人工；</li>
 *   <li>引用由服务端生成并随回答返回，模型只负责组织句子，不允许产生引用。</li>
 * </ol>
 *
 * <p>为什么检索模式必须显式：缺 embedding 时"自动退回关键词"会让运营以为语义检索在工作，
 * 实际召回质量不同；这里把模式放进配置并在 AI 状态页展示，出问题能定位到底走了哪条路。
 */
@Service
public class AiAnswerService {

    public record Citation(String sourceId, double score, String snippet) {
    }

    public record Answer(String answer, boolean fallback, String reason,
                         List<Citation> citations, double topScore,
                         String provider, String retrieval, long latencyMs) {
    }

    private static final String SYSTEM_PROMPT = """
            你是电商客服。只能依据用户消息里的【知识片段】回答，不得使用片段之外的知识。
            规则：
            1. 不编造价格、时效、退换政策；片段没有的就不要补充；
            2. 用简体中文，2-4 句，直接给结论，不要复述规则；
            3. 片段不足以回答时，只输出：无法回答；
            4. 不要输出内部字段名、接口路径或编号。""";

    private final JdbcTemplate jdbc;
    private final AiMediaService media;
    private final SemanticIndexService index;
    private final KeywordRetriever keyword;
    private final AuditEventService auditEvents;
    private final String retrieval;
    private final double minScore;
    private final double keywordMinScore;
    private final int topK;

    public AiAnswerService(JdbcTemplate jdbc, AiMediaService media, SemanticIndexService index,
                           KeywordRetriever keyword, AuditEventService auditEvents,
                           @Value("${app.ai.ask.retrieval}") String retrieval,
                           @Value("${app.ai.ask.min-score:0.72}") double minScore,
                           @Value("${app.ai.ask.keyword-min-score}") double keywordMinScore,
                           @Value("${app.ai.ask.top-k:5}") int topK,
                           @Value("${app.ai.ask.banned-words:}") String bannedWords) {
        this.jdbc = jdbc;
        this.media = media;
        this.index = index;
        this.keyword = keyword;
        this.auditEvents = auditEvents;
        this.retrieval = normalizeRetrieval(retrieval);
        this.minScore = minScore;
        this.keywordMinScore = keywordMinScore;
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
            return fallback("问题包含不当内容，已转人工处理", List.of(), 0.0, null, 0);
        }

        String provider = chatProviderCode();
        List<Citation> citations = retrieve(question);
        if (citations.isEmpty()) {
            return refuse(actor, question, 0.0, "检索结果低于可信阈值");
        }

        long startedAt = System.nanoTime();
        String answer;
        try {
            answer = media.chat(buildPrompt(question, citations), SYSTEM_PROMPT, provider);
        } catch (ApiException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            // 模型不可达/超时/返回空：明确 503，由管理员看到真实原因；不返回"没有答案"的假象
            throw ApiException.serviceUnavailable("AI_CHAT_FAILED",
                    "对话模型调用失败（provider=" + provider + "）：" + failure.getMessage());
        }
        long latencyMs = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
        String trimmed = answer == null ? "" : answer.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.serviceUnavailable("AI_CHAT_EMPTY",
                    "对话模型返回空内容（provider=" + provider + "）");
        }

        if (trimmed.contains("无法回答")) {
            auditEvents.record(new AuditEventService.AuditEvent("AI_KNOWLEDGE_REFUSED", actor,
                    "ai_ask", null, "reason=模型判断片段不足 provider=" + provider
                            + " q=" + shorten(question), null));
            return fallback("模型判断知识片段不足以回答", citations, citations.get(0).score(),
                    provider, latencyMs);
        }

        String answerBanned = firstBanned(trimmed);
        if (answerBanned != null) {
            auditEvents.record(new AuditEventService.AuditEvent("AI_ANSWER_BLOCKED", actor,
                    "ai_ask", null, "hit=" + answerBanned + " q=" + shorten(question), null));
            return fallback("回答命中内容约束，已转人工处理", citations, citations.get(0).score(),
                    provider, latencyMs);
        }

        auditEvents.record(new AuditEventService.AuditEvent("AI_KNOWLEDGE_ANSWERED", actor,
                "ai_ask", null, "provider=" + provider + " retrieval=" + retrieval
                + " citations=" + citations.size() + " topScore=" + citations.get(0).score()
                + " latencyMs=" + latencyMs, null));
        return new Answer(trimmed, false, null, citations, citations.get(0).score(),
                provider, retrieval, latencyMs);
    }

    /** 检索结果转引用：阈值按模式取（关键词分与余弦分不是同一个尺度）。 */
    private List<Citation> retrieve(String question) {
        double threshold = "SEMANTIC".equals(retrieval) ? minScore : keywordMinScore;
        List<Citation> citations = new ArrayList<>();
        if ("SEMANTIC".equals(retrieval)) {
            if (!index.enabled()) {
                throw ApiException.serviceUnavailable("AI_SEMANTIC_DISABLED",
                        "语义检索未开启：设置 APP_AI_SEMANTIC_ENABLED=true，或把 APP_AI_ASK_RETRIEVAL 改为 KEYWORD");
            }
            List<Map<String, Object>> hits;
            try {
                hits = index.search(question.trim(), "FAQ", topK, null);
            } catch (ApiException failure) {
                if ("SEMANTIC_INDEX_EMPTY".equals(failure.code())) {
                    // 索引为空是"还没有知识"，不是"没装好"：交给上层按拒答处理，不要 503 吓人
                    return List.of();
                }
                if ("AI_EMBEDDING_UNAVAILABLE".equals(failure.code())) {
                    throw ApiException.serviceUnavailable("AI_EMBEDDING_NOT_CONFIGURED",
                            "未配置向量模型（protocol=openai-embedding）；没有 embedding 时把 APP_AI_ASK_RETRIEVAL 设为 KEYWORD");
                }
                throw failure;
            }
            for (Map<String, Object> hit : hits) {
                double score = ((Number) hit.get("score")).doubleValue();
                if (score >= threshold) {
                    citations.add(new Citation(String.valueOf(hit.get("sourceId")), score,
                            shorten(String.valueOf(hit.get("content")))));
                }
            }
            return citations;
        }
        for (KeywordRetriever.Hit hit : keyword.search(question.trim(), null, topK)) {
            if (hit.score() >= threshold) {
                citations.add(new Citation(hit.sourceId(), hit.score(), shorten(hit.content())));
            }
        }
        return citations;
    }

    private String buildPrompt(String question, List<Citation> citations) {
        StringBuilder prompt = new StringBuilder("【知识片段】\n");
        for (int i = 0; i < citations.size(); i++) {
            prompt.append('[').append(i + 1).append("] ")
                    .append(citations.get(i).snippet()).append('\n');
        }
        prompt.append("\n顾客问题：").append(question.trim());
        return prompt.toString();
    }

    private Answer fallback(String reason, List<Citation> citations, double topScore,
                            String provider, long latencyMs) {
        return new Answer(null, true, reason, citations, topScore, provider, retrieval, latencyMs);
    }

    private Answer refuse(String actor, String question, double topScore, String reason) {
        auditEvents.record(new AuditEventService.AuditEvent("AI_KNOWLEDGE_REFUSED", actor,
                "ai_ask", null, "reason=" + reason + " topScore=" + topScore
                + " q=" + shorten(question), null));
        return fallback(reason, List.of(), topScore, chatProviderCode(), 0);
    }

    /** 只按协议挑对话供应商：TEXT 里还有 embedding，按 kind 盲选会把问题发给向量模型。 */
    private String chatProviderCode() {
        List<String> providers = jdbc.queryForList(
                "SELECT code FROM ai_provider WHERE enabled AND protocol = 'openai-chat' "
                        + "ORDER BY priority, code LIMIT 1", String.class);
        if (providers.isEmpty()) {
            throw ApiException.serviceUnavailable("AI_CHAT_NOT_CONFIGURED",
                    "未配置对话模型：请到「AI 客服 → 模型供应商」启用一个 protocol=openai-chat 的供应商");
        }
        return providers.get(0);
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

    private static String normalizeRetrieval(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("SEMANTIC") && !normalized.equals("KEYWORD")) {
            throw new IllegalArgumentException(
                    "app.ai.ask.retrieval 只能是 SEMANTIC 或 KEYWORD，实际=" + value);
        }
        return normalized;
    }

    private String firstBanned(String text) {
        if (text == null) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String word : bannedWords) {
            if (!word.isEmpty() && lower.contains(word.toLowerCase(Locale.ROOT))) {
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