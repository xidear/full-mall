package com.fullmall.promotion.api;

import com.fullmall.promotion.app.PromotionAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 后台促销规则构建器：结构化字段，服务端生成 JSON。 */
@RestController
@RequestMapping("/admin/v1/promotions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final PromotionAdminService service;
    private final com.fullmall.promotion.app.PromotionService promotions;

    public AdminPromotionController(PromotionAdminService service,
                                    com.fullmall.promotion.app.PromotionService promotions) {
        this.service = service;
        this.promotions = promotions;
    }

    public record CreateRule(String code, String title, String type, long thresholdAmount, long value,
                             String scope, List<UUID> scopeIds, String channel,
                             Instant startsAt, Instant endsAt, int priority) {
    }

    /** 促销列表（全部类型、全部状态，字段与前端 PromotionView 对齐）。 */
    @GetMapping
    public java.util.List<com.fullmall.promotion.api.PromotionDtos.PromotionView> listAll() {
        return promotions.listAll();
    }

    /** 新建促销（秒杀/买赠/拼团），与顾客端同一套校验。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public com.fullmall.promotion.api.PromotionDtos.PromotionView createPromotion(
            @RequestBody com.fullmall.promotion.api.PromotionDtos.CreatePromotionRequest request) {
        return promotions.create(request);
    }

    /** 启用 / 结束。 */
    @PostMapping("/{id}/status")
    public com.fullmall.promotion.api.PromotionDtos.PromotionView changeStatus(
            @PathVariable UUID id, @RequestParam String status) {
        return promotions.updateStatus(id, com.fullmall.promotion.domain.PromotionStatus.valueOf(
                status == null ? "" : status.trim().toUpperCase(java.util.Locale.ROOT)));
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> create(@RequestBody CreateRule body, Authentication authentication) {
        UUID id = service.create(new PromotionAdminService.Command(
                body.code(), body.title(), body.type(), body.thresholdAmount(), body.value(),
                body.scope(), body.scopeIds(), body.channel(), body.startsAt(), body.endsAt(),
                body.priority()), authentication.getName());
        return Map.of("id", id);
    }

    @GetMapping("/rules")
    public java.util.List<PromotionAdminService.RuleView> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return service.list(status, limit);
    }

    @PutMapping("/rules/{id}")
    public void update(@PathVariable UUID id, @RequestBody CreateRule body,
                       Authentication authentication) {
        service.update(id, new PromotionAdminService.Command(
                body.code(), body.title(), body.type(), body.thresholdAmount(), body.value(),
                body.scope(), body.scopeIds(), body.channel(), body.startsAt(), body.endsAt(),
                body.priority()), authentication.getName());
    }

    @PostMapping("/rules/{id}/activate")
    public void activate(@PathVariable UUID id, Authentication authentication) {
        service.activate(id, authentication.getName());
    }

    @PostMapping("/rules/{id}/end")
    public void end(@PathVariable UUID id, Authentication authentication) {
        service.end(id, authentication.getName());
    }
}
