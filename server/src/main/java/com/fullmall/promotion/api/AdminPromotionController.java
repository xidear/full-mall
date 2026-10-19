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
@RequestMapping("/admin/v1/promotions/rules")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final PromotionAdminService service;

    public AdminPromotionController(PromotionAdminService service) {
        this.service = service;
    }

    public record CreateRule(String code, String title, String type, long thresholdAmount, long value,
                             String scope, List<UUID> scopeIds, String channel,
                             Instant startsAt, Instant endsAt, int priority) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> create(@RequestBody CreateRule body, Authentication authentication) {
        UUID id = service.create(new PromotionAdminService.Command(
                body.code(), body.title(), body.type(), body.thresholdAmount(), body.value(),
                body.scope(), body.scopeIds(), body.channel(), body.startsAt(), body.endsAt(),
                body.priority()), authentication.getName());
        return Map.of("id", id);
    }

    @PostMapping("/{id}/activate")
    public void activate(@PathVariable UUID id, Authentication authentication) {
        service.activate(id, authentication.getName());
    }

    @PostMapping("/{id}/end")
    public void end(@PathVariable UUID id, Authentication authentication) {
        service.end(id, authentication.getName());
    }
}
