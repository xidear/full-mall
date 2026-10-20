package com.fullmall.fulfillment.api;

import com.fullmall.fulfillment.app.FulfillmentService;
import com.fullmall.fulfillment.domain.FulfillmentOrder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/fulfillments")
public class CustomerFulfillmentController {

    private final FulfillmentService fulfillmentService;
    private final com.fullmall.platform.identity.OrderOwnershipGuard ownership;

    public CustomerFulfillmentController(FulfillmentService fulfillmentService,
                                         com.fullmall.platform.identity.OrderOwnershipGuard ownership) {
        this.fulfillmentService = fulfillmentService;
        this.ownership = ownership;
    }

    /**
     * 订单履约信息（含承运商/运单号/**自提码**）。
     *
     * <p>必须校验归属：会员订单只允许本人，游客订单必须带联系人手机号——
     * 自提码可直接取货，不能仅凭订单号泄露。
     */
    @GetMapping
    public List<FulfillmentOrder> list(@PathVariable UUID orderId,
                                       @RequestParam(required = false) String phone,
                                       @RequestHeader(value = "X-Order-Token", required = false)
                                       String orderToken,
                                       org.springframework.security.core.Authentication authentication) {
        UUID memberId = authentication == null || authentication.getName() == null
                ? null : UUID.fromString(authentication.getName());
        ownership.requireAccess(orderId, memberId, phone, orderToken);
        return fulfillmentService.findFulfillments(orderId);
    }
}