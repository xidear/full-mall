package com.fullmall.promotion.api;

import com.fullmall.promotion.api.PromotionDtos.CreatePromotionRequest;
import com.fullmall.promotion.api.PromotionDtos.PromotionView;
import com.fullmall.promotion.app.PromotionAdminService;
import com.fullmall.promotion.app.PromotionService;
import com.fullmall.promotion.domain.PromotionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 后台促销管理。
 *
 * <p>鉴权由 {@code StaffAuthFilter} 统一处理：路径 `/admin/v1/promotions` 映射到权限码
 * `promotion.manage`，ADMIN 直接放行。**这里刻意不写 `@PreAuthorize`**——
 * 实测方法级 `hasRole('ADMIN')` 在该过滤器建立的认证上会被拒（拿不到预期 authority），
 * 导致接口 500；权限判断只保留一条路径，避免两套模型互相打架。
 */
@RestController
@RequestMapping("/admin/v1/promotions")
public class AdminPromotionController {

    private final PromotionAdminService rules;
    private final PromotionService promotions;

    public AdminPromotionController(PromotionAdminService rules, PromotionService promotions) {
        this.rules = rules;
        this.promotions = promotions;
    }

    public record CreateRule(String code, String title, String type, long thresholdAmount, long value,
                             String scope, List<UUID> scopeIds, String channel,
                             Instant startsAt, Instant endsAt, int priority) {
    }

    /** 促销列表（全部类型、全部状态，字段与前端 PromotionView 对齐）。 */
    @GetMapping
    public List<PromotionView> listAll() {
        return promotions.listAll();
    }

    /** 新建促销（秒杀/买赠/拼团），与顾客端同一套校验。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionView createPromotion(@RequestBody CreatePromotionRequest request) {
        return promotions.create(request);
    }

    /** 启用 / 结束。 */
    @PostMapping("/{id}/status")
    public PromotionView changeStatus(@PathVariable UUID id, @RequestParam String status) {
        if (status == null || status.isBlank()) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "STATUS_REQUIRED", "必须指定目标状态");
        }
        return promotions.updateStatus(id, PromotionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)));
    }

    // ---- 满减/满折规则（结构化表单，JSON 由服务端生成） ----

    @GetMapping("/rules")
    public List<PromotionAdminService.RuleView> listRules(@RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "50") int limit) {
        return rules.list(status, limit);
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> createRule(@RequestBody CreateRule body, Authentication authentication) {
        UUID id = rules.create(new PromotionAdminService.Command(
                body.code(), body.title(), body.type(), body.thresholdAmount(), body.value(),
                body.scope(), body.scopeIds(), body.channel(), body.startsAt(), body.endsAt(),
                body.priority()), authentication.getName());
        return Map.of("id", id);
    }

    @PutMapping("/rules/{id}")
    public void updateRule(@PathVariable UUID id, @RequestBody CreateRule body,
                           Authentication authentication) {
        rules.update(id, new PromotionAdminService.Command(
                body.code(), body.title(), body.type(), body.thresholdAmount(), body.value(),
                body.scope(), body.scopeIds(), body.channel(), body.startsAt(), body.endsAt(),
                body.priority()), authentication.getName());
    }

    @PostMapping("/rules/{id}/activate")
    public void activateRule(@PathVariable UUID id, Authentication authentication) {
        rules.activate(id, authentication.getName());
    }

    @PostMapping("/rules/{id}/end")
    public void endRule(@PathVariable UUID id, Authentication authentication) {
        rules.end(id, authentication.getName());
    }
}
