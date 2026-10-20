package com.fullmall.coupon.api;

import com.fullmall.coupon.app.CouponService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 优惠券：后台建模板/启停，会员领券与查看。 */
@RestController
public class CouponController {

    private final CouponService coupons;

    public CouponController(CouponService coupons) {
        this.coupons = coupons;
    }

    public record CreateTemplate(String code, String title, String type, long value, long minOrderAmount,
                                 int totalQuantity, int perMemberLimit, int validityDays,
                                 Instant startsAt, Instant endsAt) {
    }

    private UUID member(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw com.fullmall.platform.error.ApiException.badRequest("MEMBER_REQUIRED", "需要会员登录");
        }
        return UUID.fromString(authentication.getName());
    }

    @PostMapping("/admin/v1/coupons/templates")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> createTemplate(@RequestBody CreateTemplate body) {
        UUID id = coupons.createTemplate(body.code(), body.title(), body.type(), body.value(),
                body.minOrderAmount(), body.totalQuantity(), body.perMemberLimit(), body.validityDays(),
                body.startsAt(), body.endsAt());
        return Map.of("id", id);
    }

    @GetMapping("/admin/v1/coupons/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CouponService.TemplateView> listTemplates(
            @RequestParam(defaultValue = "100") int limit) {
        return coupons.listTemplates(limit);
    }

    @PostMapping("/admin/v1/coupons/templates/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public void activate(@PathVariable UUID id) {
        coupons.activate(id);
    }

    @PostMapping("/api/v1/coupons/{templateId}/claim")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> claim(@PathVariable UUID templateId,
                                   org.springframework.security.core.Authentication authentication) {
        return Map.of("id", coupons.claim(templateId, member(authentication)));
    }

    /** 校验券对某金额是否可用（只读，不改状态、不改金额）。 */
    @GetMapping("/api/v1/coupons/{couponId}/eligibility")
    public CouponService.Eligibility eligibility(@PathVariable UUID couponId,
                                                 @RequestParam long orderAmount,
                                                 org.springframework.security.core.Authentication authentication) {
        return coupons.checkUsable(couponId, member(authentication), orderAmount);
    }

    @GetMapping("/api/v1/coupons/mine")
    public List<CouponService.Coupon> mine(@RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "20") int limit,
                                           org.springframework.security.core.Authentication authentication) {
        return coupons.listMine(member(authentication), status, limit);
    }
}
