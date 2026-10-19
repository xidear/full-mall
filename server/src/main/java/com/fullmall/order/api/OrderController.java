package com.fullmall.order.api;

import com.fullmall.order.app.OrderService;
import com.fullmall.order.domain.Order;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    private final com.fullmall.order.infra.OrderRepository orders;

    public OrderController(OrderService orderService, com.fullmall.order.infra.OrderRepository orders) {
        this.orderService = orderService;
        this.orders = orders;
    }

    /**
     * 订单详情。**已绑定会员的订单只允许本人查看**（越权统一 404，不暴露存在性）；
     * 游客订单保持原有可通过订单号查看的行为（无需登录）。
     */
    @GetMapping("/{orderId}")
    public Order get(@PathVariable UUID orderId,
                     org.springframework.security.core.Authentication authentication) {
        UUID ownerMemberId = orders.memberIdOf(orderId);
        if (ownerMemberId != null) {
            String caller = authentication == null ? null : authentication.getName();
            if (caller == null || !ownerMemberId.toString().equals(caller)) {
                throw com.fullmall.platform.error.ApiException.notFound(
                        "ORDER_NOT_FOUND", "订单不存在: " + orderId);
            }
        }
        return orderService.getOrder(orderId);
    }
}