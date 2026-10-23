package com.fullmall.analytics.api;

import com.fullmall.analytics.action.AiActionDraftService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** AI 受控写操作：创建草稿、双签审批、执行（仅后台管理员）。 */
@RestController
@RequestMapping("/admin/v1/ai/actions")
@PreAuthorize("hasRole('ADMIN')")
public class AiActionController {

    private final AiActionDraftService drafts;
    private final com.fullmall.analytics.action.AiDraftProposer proponer;

    public AiActionController(AiActionDraftService drafts,
                              org.springframework.beans.factory.ObjectProvider<
                                      com.fullmall.analytics.ai.OpenAiCompatClient> aiClients) {
        this.drafts = drafts;
        this.proponer = new com.fullmall.analytics.action.AiDraftProposer(
                aiClients.getIfAvailable(), drafts);
    }

    public record CreateRequest(String actionType, Map<String, Object> payload) {
    }

    public record RejectRequest(String reason) {
    }

    public record ProposeRequest(String instruction) {
    }

    /** 自然语言 → 草稿（仍需双签）；未配置模型明确 503。 */
    @PostMapping("/propose")
    public AiActionDraftService.Draft propose(@RequestBody ProposeRequest body,
                                              Authentication authentication) {
        return proponer.propose(body.instruction(), authentication.getName());
    }

    @PostMapping
    public AiActionDraftService.Draft create(@RequestBody CreateRequest body,
                                             Authentication authentication) {
        return drafts.create(body.actionType(), body.payload(), authentication.getName());
    }

    @PostMapping("/{id}/approve")
    public AiActionDraftService.Draft approve(@PathVariable UUID id, Authentication authentication) {
        return drafts.approve(id, authentication.getName());
    }

    @PostMapping("/{id}/reject")
    public AiActionDraftService.Draft reject(@PathVariable UUID id, @RequestBody(required = false) RejectRequest body,
                                             Authentication authentication) {
        return drafts.reject(id, authentication.getName(), body == null ? null : body.reason());
    }

    @PostMapping("/{id}/execute")
    public AiActionDraftService.Draft execute(@PathVariable UUID id, Authentication authentication) {
        return drafts.execute(id, authentication.getName());
    }

    @GetMapping("/pending")
    public List<Map<String, Object>> pending(@RequestParam(defaultValue = "50") int limit) {
        return drafts.pending(limit);
    }
}
