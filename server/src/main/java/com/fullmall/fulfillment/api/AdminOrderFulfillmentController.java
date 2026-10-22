package com.fullmall.fulfillment.api;

import com.fullmall.fulfillment.app.FulfillmentService;
import com.fullmall.fulfillment.domain.FulfillmentOrder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 后台/客服查看订单履约信息。
 *
 * <p>为什么不复用顾客接口：顾客接口要求"会员本人或订单手机号"，后台客服没有这两样；
 * 曾经用"顾客接口 + 手机号"绕过，属于权限模型不干净。后台走自己的路径，
 * 由 `StaffAuthFilter` 按 `/admin/v1/orders` → `order.manage` 校验员工权限。
 */
@RestController
@RequestMapping("/admin/v1/orders/{orderId}/fulfillments")
public class AdminOrderFulfillmentController {

    private final FulfillmentService fulfillmentService;

    public AdminOrderFulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping
    public List<FulfillmentOrder> list(@PathVariable UUID orderId) {
        return fulfillmentService.findFulfillments(orderId);
    }

    public record CancelRequest(String reason) {
    }

    /** 取消履约单：仅未开始拣货可取消，会释放库存预占（退款走售后）。 */
    @PostMapping("/{fulfillmentId}/cancel")
    public FulfillmentOrder cancel(@PathVariable UUID orderId,
                                   @PathVariable UUID fulfillmentId,
                                   @RequestBody(required = false) CancelRequest body) {
        return fulfillmentService.cancelFulfillment(fulfillmentId,
                body == null ? null : body.reason());
    }
}
