package com.fullmall.ai.media.api;

import com.fullmall.ai.media.AiMediaService;
import com.fullmall.ai.media.MediaKind;
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

/** AI 素材生产：供应商、任务、提示词历史、提示词优化（全部后台）。 */
@RestController
@RequestMapping("/admin/v1/ai/media")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class AiMediaController {

    private final AiMediaService media;

    public AiMediaController(AiMediaService media) {
        this.media = media;
    }

    public record CreateJobRequest(MediaKind kind, String providerCode, UUID productId, UUID skuId,
                                   String purpose, String prompt, String negative,
                                   Map<String, Object> params, List<UUID> sourceAssetIds) {
    }

    public record OptimizeRequest(String prompt, String providerCode) {
    }

    @GetMapping("/providers")
    public List<Map<String, Object>> providers(@RequestParam(required = false) MediaKind kind) {
        return media.providers(kind);
    }

    @PostMapping("/jobs")
    public Map<String, Object> create(@RequestBody CreateJobRequest body, Authentication auth) {
        UUID id = media.enqueue(new AiMediaService.CreateJob(body.kind(), body.providerCode(),
                body.productId(), body.skuId(), body.purpose(), body.prompt(), body.negative(),
                body.params(), body.sourceAssetIds()), auth.getName());
        return Map.of("jobId", id.toString(), "status", "PENDING");
    }

    @GetMapping("/jobs/{id}")
    public Map<String, Object> job(@PathVariable UUID id) {
        return media.job(id);
    }

    @GetMapping("/jobs")
    public List<Map<String, Object>> jobs(@RequestParam(defaultValue = "20") int limit) {
        return media.recentJobs(limit);
    }

    @GetMapping("/prompts")
    public List<Map<String, Object>> prompts(@RequestParam(required = false) UUID productId,
                                             @RequestParam(required = false) UUID skuId,
                                             @RequestParam(defaultValue = "20") int limit) {
        return media.promptHistory(productId, skuId, limit);
    }

    @PostMapping("/prompts/optimize")
    public Map<String, Object> optimize(@RequestBody OptimizeRequest body) {
        return Map.of("prompt", media.optimizePrompt(body.prompt(), body.providerCode()));
    }
}
