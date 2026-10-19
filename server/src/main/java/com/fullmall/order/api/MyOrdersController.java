package com.fullmall.order.api;

import com.fullmall.member.auth.MemberAuthService;
import com.fullmall.order.infra.OrderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 我的订单：按登录会员的手机号查询，只返回摘要（不含他人订单）。 */
@RestController
public class MyOrdersController {

    private final OrderRepository orders;
    private final MemberAuthService memberAuth;

    public MyOrdersController(OrderRepository orders, MemberAuthService memberAuth) {
        this.orders = orders;
        this.memberAuth = memberAuth;
    }

    @GetMapping("/api/v1/orders/mine")
    public List<OrderRepository.OrderSummary> mine(
            @RequestParam(defaultValue = "20") int limit, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "MEMBER_REQUIRED", "需要会员登录");
        }
        if (limit <= 0 || limit > 100) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "INVALID_LIMIT", "查询条数必须在 1..100，实际=" + limit);
        }
        String phone = memberAuth.phoneOf(UUID.fromString(authentication.getName()));
        return orders.findByContactPhone(phone, limit);
    }
}
