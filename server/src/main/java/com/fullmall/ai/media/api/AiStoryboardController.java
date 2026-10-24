package com.fullmall.ai.media.api;

import com.fullmall.ai.media.AiStoryboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 视频分镜：建分镜、看镜头、逐镜头生成、回填结果。 */
@RestController
@RequestMapping("/admin/v1/ai/storyboards")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class AiStoryboardController {

    private final AiStoryboardService storyboards;

    public AiStoryboardController(AiStoryboardService storyboards) {
        this.storyboards = storyboards;
    }

    public record CreateRequest(String title, UUID productId, UUID skuId,
                                List<AiStoryboardService.ShotInput> shots) {
    }

    public record GenerateRequest(String providerCode) {
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody CreateRequest body, Authentication auth) {
        UUID id = storyboards.create(body.title(), body.productId(), body.skuId(),
                body.shots(), auth.getName());
        return Map.of("storyboardId", id.toString());
    }

    @GetMapping("/{id}")
    public AiStoryboardService.Storyboard get(@PathVariable UUID id) {
        return storyboards.get(id);
    }

    @PostMapping("/{id}/generate")
    public Map<String, Object> generate(@PathVariable UUID id,
                                        @RequestBody(required = false) GenerateRequest body,
                                        Authentication auth) {
        var jobIds = storyboards.generate(id, body == null ? null : body.providerCode(),
                auth.getName());
        return Map.of("jobIds", jobIds.stream().map(UUID::toString).toList());
    }

    @PostMapping("/{id}/sync")
    public Map<String, Object> sync(@PathVariable UUID id) {
        return Map.of("synced", storyboards.syncAssets(id));
    }
}
