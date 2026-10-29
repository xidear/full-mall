package com.fullmall.order.api;

import com.fullmall.order.app.OrderService;
import com.fullmall.order.domain.Order;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 越权与不存在统一 404（403 等于承认订单存在）；订单头与订单行是两次查询、无共享快照，别拿来做资金核对。
 */
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
     * 订单详情。已绑定会员的订单只允许本人查看（越权统一 404，不暴露存在性）；
     * 游客订单保持原有可通过订单号查看的行为（无需登录）。
     *
     * <p>返回完整 Order（含地址、联系电话）；游客订单未接 OrderOwnershipGuard，知道订单号即可匿名查看。
     */
    @GetMapping("/{orderId}")
    public Order get(@PathVariable UUID orderId, Authentication authentication) {
        return load(orderId, authentication);
    }

    /** 顾客端地址使用业务订单号（如 FM202610280001），不把数据库 UUID 放进 URL。 */
    @GetMapping("/no/{orderNo}")
    public Order getByOrderNo(@PathVariable String orderNo, Authentication authentication) {
        UUID orderId = orders.idByOrderNo(orderNo);
        // null 只表示"该订单号不存在"（order_no 有唯一约束），不代表游客订单——这里必须报错，不能放行
        if (orderId == null) {
            throw com.fullmall.platform.error.ApiException.notFound(
                    "ORDER_NOT_FOUND", "订单不存在");
        }
        return load(orderId, authentication);
    }

    private Order load(UUID orderId, Authentication authentication) {
        // null 表示游客订单（member_id IS NULL），与"订单不存在"语义不同；订单不存在时由 getOrder 抛 404
        UUID ownerMemberId = orders.memberIdOf(orderId);
        if (ownerMemberId != null) {
            // principal 是会员 UUID（MemberAuthFilter 设定）；员工 principal 是用户名，后台查已绑定订单会得 404
            String caller = authentication == null ? null : authentication.getName();
            if (caller == null || !ownerMemberId.toString().equals(caller)) {
                throw com.fullmall.platform.error.ApiException.notFound(
                        "ORDER_NOT_FOUND", "订单不存在");
            }
        }
        return orderService.getOrder(orderId);
    }
}
