package com.fullmall.ai.status;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 能力状态（后台 AI 客服页据此决定展示什么）。
 *
 * <p>必须把"构建没有该模块"和"模块在但没配好"分开：
 * <ul>
 *   <li>{@code edition}：构建形态（internal / community）；</li>
 *   <li>{@code enabled}：库里是否已有可用供应商；</li>
 *   <li>{@code ready}：按当前检索模式，问答链路是否真的能跑（KEYWORD 不要求 embedding）。</li>
 * </ul>
 *
 * <p>前端按 {@code retrieval} 展示"语义检索 / 关键词检索"，避免运营以为向量检索在工作。
 */
@RestController
@RequestMapping("/admin/v1/ai")
public class AiStatusController {

    public record AiStatusView(String edition, boolean enabled, int providerCount, boolean ready,
                               String reason, List<String> missing, String chatProvider,
                               String chatModel, String embeddingModel, String retrieval,
                               boolean semanticEnabled, int topK) {
    }

    private final JdbcTemplate jdbc;
    private final String edition;
    private final String retrieval;
    private final boolean semanticEnabled;
    private final int topK;

    public AiStatusController(JdbcTemplate jdbc, @Value("${app.edition:internal}") String edition,
                              @Value("${app.ai.ask.retrieval}") String retrieval,
                              @Value("${app.ai.semantic.enabled}") boolean semanticEnabled,
                              @Value("${app.ai.ask.top-k}") int topK) {
        this.jdbc = jdbc;
        this.edition = edition;
        this.retrieval = retrieval == null ? "" : retrieval.trim().toUpperCase(java.util.Locale.ROOT);
        this.semanticEnabled = semanticEnabled;
        this.topK = topK;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public AiStatusView status() {
        List<Map<String, Object>> providers = jdbc.query("""
                SELECT code, name, protocol, model, enabled FROM ai_provider
                 ORDER BY priority, code
                """, (rs, rowNum) -> Map.of(
                "code", rs.getString("code"),
                "name", rs.getString("name"),
                "protocol", rs.getString("protocol"),
                "model", rs.getString("model"),
                "enabled", rs.getBoolean("enabled")));

        Map<String, Object> chat = firstEnabled(providers, "openai-chat");
        Map<String, Object> embedding = firstEnabled(providers, "openai-embedding");
        List<String> missing = new ArrayList<>();
        if (chat == null) {
            missing.add("对话模型（protocol=openai-chat）");
        }
        if ("SEMANTIC".equals(retrieval) && embedding == null) {
            missing.add("向量模型（protocol=openai-embedding）");
        }
        boolean ready = missing.isEmpty();
        String reason;
        if (ready) {
            reason = null;
        } else if (chat == null) {
            reason = "未配置对话模型：到「AI 客服 → 模型供应商」添加一个 openai-chat 供应商";
        } else {
            reason = "当前检索模式为 SEMANTIC，但缺少向量模型；可配置 embedding，或把 APP_AI_ASK_RETRIEVAL 改为 KEYWORD";
        }
        int enabledCount = (int) providers.stream()
                .filter(provider -> Boolean.TRUE.equals(provider.get("enabled")))
                .count();
        return new AiStatusView(edition, enabledCount > 0, providers.size(), ready, reason, missing,
                chat == null ? null : String.valueOf(chat.get("code")),
                chat == null ? null : String.valueOf(chat.get("model")),
                embedding == null ? null : String.valueOf(embedding.get("model")),
                retrieval, semanticEnabled, topK);
    }

    private static Map<String, Object> firstEnabled(List<Map<String, Object>> providers,
                                                    String protocol) {
        return providers.stream()
                .filter(provider -> Boolean.TRUE.equals(provider.get("enabled")))
                .filter(provider -> protocol.equals(provider.get("protocol")))
                .findFirst()
                .orElse(null);
    }
}