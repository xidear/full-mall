package com.fullmall.support.api;

import com.fullmall.support.app.FaqService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** FAQ：前台检索（记录未命中）+ 后台候选与采纳。 */
@RestController
public class FaqController {

    private final FaqService faq;
    private final com.fullmall.ai.knowledge.SemanticIndexService semantic;

    public FaqController(FaqService faq,
                         com.fullmall.ai.knowledge.SemanticIndexService semantic) {
        this.faq = faq;
        this.semantic = semantic;
    }

    public record AdoptRequest(String question, String answer) {
    }

    @GetMapping("/api/v1/faq/search")
    public Map<String, Object> search(@RequestParam String q,
                                      @RequestParam(required = false) UUID memberId) {
        var hit = faq.search(q, memberId);
        if (hit == null) {
            // 明确告知未命中，让前端走人工客服，而不是假装有答案
            return Map.of("matched", false);
        }
        return Map.of("matched", true, "question", hit.question(), "answer", hit.answer());
    }

    /** 语义检索：需开启 app.ai.semantic.enabled，否则明确 409（不偷偷退关键词）。 */
    @GetMapping("/api/v1/faq/semantic")
    public java.util.List<java.util.Map<String, Object>> semanticSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {
        return semantic.search(q, "FAQ", limit, null);
    }

    @GetMapping("/admin/v1/faq")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','SUPPORT')")
    public List<FaqService.Hit> list(@RequestParam(required = false) String keyword) {
        return faq.list(keyword);
    }

    @GetMapping("/admin/v1/faq/candidates")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','SUPPORT')")
    public List<Map<String, Object>> candidates(@RequestParam(defaultValue = "30") int days,
                                                @RequestParam(defaultValue = "2") int minCount) {
        return faq.candidates(days, minCount);
    }

    @PostMapping("/admin/v1/faq")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','SUPPORT')")
    public FaqService.Hit adopt(@RequestBody AdoptRequest body) {
        return faq.adopt(body.question(), body.answer());
    }
}
