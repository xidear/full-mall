package com.fullmall.order.api;

import com.fullmall.order.app.OrderService;
import com.fullmall.order.domain.Order;
import com.fullmall.platform.identity.OrderOwnershipGuard;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /** 归属校验：会员单只认本人，游客单要令牌或下单手机号，否则订单号可被猜到就等于公开 PII 与提货码。 */
    private final OrderOwnershipGuard ownership;

    public OrderController(OrderService orderService, com.fullmall.order.infra.OrderRepository orders,
                           OrderOwnershipGuard ownership) {
        this.orderService = orderService;
        this.orders = orders;
        this.ownership = ownership;
    }

    /**
     * 订单详情。会员单只允许本人（越权统一 404，不暴露存在性）；游客单必须带 X-Order-Token
     * 或与联系人一致的手机号，否则 409/403 —— 只凭订单号就能读到 PII 与提货码是漏洞。
     */
    @GetMapping("/{orderId}")
    public Order get(@PathVariable UUID orderId, Authentication authentication,
                     @RequestHeader(value = "X-Order-Token", required = false) String orderToken,
                     @RequestParam(value = "phone", required = false) String phone) {
        return load(orderId, authentication, orderToken, phone);
    }

    /** 顾客端地址使用业务订单号（如 FM202610280001），不把数据库 UUID 放进 URL。 */
    @GetMapping("/no/{orderNo}")
    public Order getByOrderNo(@PathVariable String orderNo, Authentication authentication,
                              @RequestHeader(value = "X-Order-Token", required = false) String orderToken,
                              @RequestParam(value = "phone", required = false) String phone) {
        UUID orderId = orders.idByOrderNo(orderNo);
        // null 只表示"该订单号不存在"（order_no 有唯一约束），不代表游客订单——这里必须报错，不能放行
        if (orderId == null) {
            throw com.fullmall.platform.error.ApiException.notFound(
                    "ORDER_NOT_FOUND", "订单不存在");
        }
        return load(orderId, authentication, orderToken, phone);
    }

    private Order load(UUID orderId, Authentication authentication, String orderToken, String phone) {
        // 会员单：principal 是会员 UUID（MemberAuthFilter 设定）；员工 principal 是用户名，会被当成“非本人”得 404，
        // 后台查订单要走 /admin/v1 接口。游客单：令牌或下单手机号任一匹配才放行（见 OrderOwnershipGuard）。
        ownership.requireAccess(orderId, memberId(authentication), phone, orderToken);
        return orderService.getOrder(orderId);
    }

    /** principal 只有会员 UUID 才当成会员身份；游客、员工用户名一律返回 null（交给游客分支判定）。 */
    private static UUID memberId(Authentication authentication) {
        String caller = authentication == null ? null : authentication.getName();
        if (caller == null || caller.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(caller);
        } catch (IllegalArgumentException notAMemberPrincipal) {
            return null;
        }
    }
}
