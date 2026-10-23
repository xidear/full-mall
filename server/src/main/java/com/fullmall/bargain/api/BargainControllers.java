package com.fullmall.bargain.api;

import org.springframework.web.bind.annotation.GetMapping;
import com.fullmall.bargain.app.BargainService;
import com.fullmall.promotion.app.BargainPricing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 砍价接口。
 *
 * <p>后台：创建活动（底价规则走 `BargainPricing`，不设成本下限，但低于成本必须确认）；
 * 顾客：发起、助力、绑单。库存由正常下单流程处理，未支付由订单超时释放。
 */
@RestController
@RequestMapping("/admin/v1/bargain-activities")
class AdminBargainController {

    private final JdbcTemplate jdbc;

    AdminBargainController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record CreateRequest(UUID skuId, long floorPrice, long maxCut, int minHelpers,
                                long cutMin, long cutMax, int expiresHours, boolean allowBelowCost,
                                String belowCostReason, Instant startsAt, Instant endsAt) {
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, Object> create(@RequestBody CreateRequest body) {
        // 先做业务校验，避免把 DB 约束违规暴露成 500
        if (body.minHelpers() < 3 || body.minHelpers() > 30) {
            throw new com.fullmall.platform.error.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "BARGAIN_HELPERS_INVALID",
                    "助力人数必须在 3–30 之间，当前=" + body.minHelpers());
        }
        if (body.expiresHours() < 12 || body.expiresHours() > 72) {
            throw new com.fullmall.platform.error.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "BARGAIN_EXPIRY_INVALID",
                    "有效期必须在 12–72 小时之间，当前=" + body.expiresHours());
        }
        if (body.floorPrice() <= 0) {
            throw new com.fullmall.platform.error.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "BARGAIN_FLOOR_INVALID",
                    "底价必须大于 0");
        }
        if (body.cutMin() <= 0 || body.cutMax() < body.cutMin()) {
            throw new com.fullmall.platform.error.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "BARGAIN_CUT_INVALID",
                    "每刀金额必须大于 0 且上限不小于下限");
        }
        Long cost = jdbc.query("""
                SELECT avg_cost_amount FROM sku_cost WHERE sku_id = ?
                """, rs -> rs.next() ? rs.getLong("avg_cost_amount") : null, body.skuId());
        var decision = BargainPricing.evaluate(body.floorPrice(), cost,
                body.allowBelowCost(), body.belowCostReason());

        String currency = jdbc.queryForObject("SELECT price_currency FROM sku WHERE id = ?",
                String.class, body.skuId());
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bargain_activity (id, sku_id, floor_price, currency, max_cut, min_helpers,
                                              cut_min, cut_max, expires_hours, allow_below_cost,
                                              below_cost_reason, status, starts_at, ends_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                """, id, body.skuId(), body.floorPrice(), currency, body.maxCut(), body.minHelpers(),
                body.cutMin(), body.cutMax(), body.expiresHours(), body.allowBelowCost(),
                body.belowCostReason(), java.sql.Timestamp.from(body.startsAt()),
                java.sql.Timestamp.from(body.endsAt()));
        return Map.of("activityId", id,
                "recommendedPrice", decision.recommendedPrice(),
                "belowCost", decision.belowCost(),
                "costUnknown", decision.costUnknown());
    }
}

@RestController
@RequestMapping("/api/v1/bargain")
class CustomerBargainController {

    private final BargainService bargain;

    CustomerBargainController(BargainService bargain) {
        this.bargain = bargain;
    }

    /** 活动详情（前台展示：标题、原价、底价、人数、时间窗）。 */
    @GetMapping("/activities/{activityId}")
    public java.util.Map<String, Object> activity(@PathVariable UUID activityId) {
        return bargain.activityDetail(activityId);
    }

    @PostMapping("/activities/{activityId}/start")
    public BargainService.Session start(@PathVariable UUID activityId,
                                        @RequestParam UUID memberId) {
        return bargain.start(activityId, memberId);
    }

    @PostMapping("/sessions/{sessionId}/help")
    public BargainService.Session help(@PathVariable UUID sessionId,
                                       @RequestParam UUID memberId) {
        return bargain.help(sessionId, memberId);
    }

    @PostMapping("/sessions/{sessionId}/order")
    public BargainService.Session bindOrder(@PathVariable UUID sessionId,
                                            @RequestParam UUID orderId) {
        return bargain.markOrdered(sessionId, orderId);
    }
}
