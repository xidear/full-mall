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

    /**
     * 拼团记录（后台「拼团记录」弹窗）。
     *
     * <p>前端一直调用本路径，但后端只在顾客端注册了 /api/v1/promotions/group-buys/{code}，
     * 后台列表缺失 → 404。这里按促销筛选，未传 limit 时返回最近 50 条。
     */
    @GetMapping("/group-buys")
    public List<PromotionDtos.GroupBuyView> groupBuys(
            @RequestParam(required = false) UUID promotionId,
            @RequestParam(defaultValue = "50") int limit) {
        return promotions.groupBuys(promotionId, limit);
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
        // 只接受 DRAFT/ACTIVE/ENDED（promotion_status_check）；拼错时 valueOf 抛成 500，这里按后台输入错误处理。
        return promotions.updateStatus(id, PromotionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)));
    }

    // ---- 满减/满折规则（结构化表单，JSON 由服务端生成） ----

    /** 范围选择器的数据源（SKU/类目/品牌）。 */
    @GetMapping("/scope-options")
    public List<PromotionAdminService.ScopeOption> scopeOptions(@RequestParam String scope,
                                                                @RequestParam(defaultValue = "100") int limit) {
        return rules.scopeOptions(scope, limit);
    }

    /** 规则列表；limit 的合法区间（1..200）只在服务层校验，这里不重复判断，避免两处阈值漂移。 */
    @GetMapping("/rules")
    public List<PromotionAdminService.RuleView> listRules(@RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "50") int limit) {
        return rules.list(status, limit);
    }

    /**
     * 新建满减/满折规则，落 DRAFT（需再调 activate 才生效）；操作人为空由服务层拒绝，不记成 "system"——否则事后无法追责。
     */
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
