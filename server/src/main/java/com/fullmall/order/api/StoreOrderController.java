package com.fullmall.order.api;

import com.fullmall.order.app.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 门店工作台代下单（场景 B：预定/预购、顾客不便自助下单）。
 *
 * <p>与顾客自助下单的区别：**不要求顾客验证码**，但必须带员工身份——订单会记录
 * `placed_by=CASHIER` 与操作员姓名，保证可追溯。
 */
@RestController
@RequestMapping("/store/v1/orders")
public class StoreOrderController {

    private final OrderService orders;

    public StoreOrderController(OrderService orders) {
        this.orders = orders;
    }

    public record Item(UUID skuId, int quantity) {
    }

    public record CreateOrder(List<Item> items, String fulfillmentMode, UUID storeId,
                              String contactName, String contactPhone,
                              String province, String city, String district, String detail) {
    }

    @PostMapping
    public com.fullmall.order.domain.Order create(@RequestBody CreateOrder body,
                                                  Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "STAFF_REQUIRED", "需要门店员工登录");
        }
        var request = new CheckoutRequest(
                body.items().stream().map(item -> new CheckoutRequest.Item(item.skuId(), item.quantity())).toList(),
                com.fullmall.order.domain.FulfillmentMode.valueOf(
                        body.fulfillmentMode() == null ? "PICKUP" : body.fulfillmentMode()),
                body.storeId(),
                new CheckoutRequest.Contact(body.contactName(), body.contactPhone(), null),
                new CheckoutRequest.Address(body.province(), body.city(), body.district(),
                        body.detail(), null),
                List.of(), null, null, null, null, null, null,
                "CASHIER", authentication.getName());
        return orders.checkout(request, null);
    }
}
