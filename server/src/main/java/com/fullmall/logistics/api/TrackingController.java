package com.fullmall.logistics.api;

import com.fullmall.logistics.app.LogisticsTrackingService;
import com.fullmall.platform.identity.OrderOwnershipGuard;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** 订单物流轨迹（顾客侧，需归属校验）。 */
@RestController
@RequestMapping("/api/v1/orders/{orderId}/tracking")
public class TrackingController {

    private final LogisticsTrackingService tracking;
    private final OrderOwnershipGuard ownership;

    public TrackingController(LogisticsTrackingService tracking, OrderOwnershipGuard ownership) {
        this.tracking = tracking;
        this.ownership = ownership;
    }

    @GetMapping
    public List<LogisticsTrackingService.TrackingEvent> track(
            @PathVariable UUID orderId,
            @RequestParam(required = false) String phone,
            Authentication authentication) {
        UUID memberId = authentication == null || authentication.getName() == null
                ? null : UUID.fromString(authentication.getName());
        ownership.requireAccess(orderId, memberId, phone);
        return tracking.trackOrder(orderId);
    }
}
