package com.fullmall.fulfillment.api;

import com.fullmall.fulfillment.app.FulfillmentService;
import com.fullmall.fulfillment.domain.FulfillmentOrder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/fulfillments")
public class CustomerFulfillmentController {

    private final FulfillmentService fulfillmentService;

    public CustomerFulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping
    public List<FulfillmentOrder> list(@PathVariable UUID orderId) {
        return fulfillmentService.findFulfillments(orderId);
    }
}