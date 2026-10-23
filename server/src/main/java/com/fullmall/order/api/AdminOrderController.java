package com.fullmall.order.api;

import com.fullmall.order.app.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final com.fullmall.coupon.app.CouponAllocationService couponAllocations;

    public AdminOrderController(OrderService orderService,
                                com.fullmall.coupon.app.CouponAllocationService couponAllocations) {
        this.orderService = orderService;
        this.couponAllocations = couponAllocations;
    }

    /** 后台订单详情（客服/运营查询，含订单行与金额）。 */
    /** 券额分摊明细（部分退货计算用）。 */
    @GetMapping("/{orderId}/coupon-allocations")
    public java.util.List<com.fullmall.coupon.app.CouponAllocationService.Allocation> couponAllocations(
            @PathVariable UUID orderId) {
        return couponAllocations.findAllocations(orderId);
    }

    @GetMapping("/{orderId}")
    public com.fullmall.order.domain.Order detail(@PathVariable UUID orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return orderService.listOrders(status, page, size, sort, direction);
    }
}