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
    private final SemanticIndexService index;
    private final AuditEventService auditEvents;
    private final double minScore;
    private final int topK;

    public AiAnswerService(JdbcTemplate jdbc, SemanticIndexService index,
                           AuditEventService auditEvents,
                           @Value("${app.ai.ask.min-score:0.72}") double minScore,
                           @Value("${app.ai.ask.top-k:5}") int topK) {
        this.jdbc = jdbc;
        this.index = index;
        this.auditEvents = auditEvents;
        this.minScore = minScore;
        this.topK = topK;
    }

    public Answer ask(String question, String actor) {
        if (question == null || question.isBlank()) {
            throw ApiException.badRequest("AI_QUESTION_REQUIRED", "问题不能为空");
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

        auditEvents.record(new AuditEventService.AuditEvent("AI_KNOWLEDGE_ANSWERED", actor,
                "ai_ask", null, "citations=" + citations.size()
                + " topScore=" + citations.get(0).score(), null));
        return new Answer(answer.toString(), false, null, citations, citations.get(0).score());
    }

    private Answer refuse(String actor, String question, double topScore, String reason) {
        auditEvents.record(new AuditEventService.AuditEvent("AI_KNOWLEDGE_REFUSED", actor,
                "ai_ask", null, "reason=" + reason + " topScore=" + topScore, null));
        return new Answer(null, true, reason, List.of(), topScore);
    }

    private String enabledTextProvider() {
        List<String> providers = jdbc.queryForList(
                "SELECT code FROM ai_provider WHERE enabled AND kind = 'TEXT' LIMIT 1", String.class);
        return providers.isEmpty() ? null : providers.get(0);
    }

    private static String shorten(String content) {
        String trimmed = content == null ? "" : content.trim();
        return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 200) + "…";
    }
}
