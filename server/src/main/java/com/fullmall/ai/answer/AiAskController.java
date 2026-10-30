package com.fullmall.ai.answer;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 客服问答。
 *
 * <p>三态是硬约定（前端与验收脚本都依赖它）：
 * <ul>
 *   <li>问题为空 → 400 {@code AI_QUESTION_REQUIRED}</li>
 *   <li>未配对话模型 / 知识库检索未开 → 503 {@code AI_PROVIDER_NOT_CONFIGURED} / {@code AI_SEMANTIC_DISABLED}</li>
 *   <li>可用 → 200；{@code fallback=true} 表示"知识库没有依据，请转人工"，此时 {@code answer} 为空</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/v1/ai")
public class AiAskController {

    public record AskRequest(String question) {
    }

    private final AiAnswerService answers;

    public AiAskController(AiAnswerService answers) {
        this.answers = answers;
    }

    /** 未命中/被拦截的问题（运营补知识库用）。 */
    @org.springframework.web.bind.annotation.GetMapping("/unanswered")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CS')")
    public java.util.List<java.util.Map<String, Object>> unanswered(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int limit) {
        return answers.recentUnanswered(limit);
    }

    @PostMapping("/ask")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CS')")
    public AiAnswerService.Answer ask(@RequestBody AskRequest body, Authentication authentication) {
        return answers.ask(body.question(), authentication.getName());
    }
}
