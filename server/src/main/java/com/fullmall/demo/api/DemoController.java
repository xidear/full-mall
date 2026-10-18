package com.fullmall.demo.api;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fullmall.fulfillment.dto.PickupDetail;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.money.Money;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mock")
@Profile({"dev", "demo"})
public class DemoController {

    @GetMapping("/checkout")
    public DemoCheckoutResult checkout(@RequestParam(defaultValue = "paid") String scenario) {
        Map<String, Object> order = Map.of(
                "orderNo", "DEMO-20261018-0001",
                "status", "PAID",
                "currency", "CNY",
                "subtotal", Map.of("amount", 1990, "currency", "CNY"),
                "shippingFee", Map.of("amount", 0, "currency", "CNY"),
                "total", Map.of("amount", 1990, "currency", "CNY"),
                "lines", List.of(Map.of(
                        "skuCode", "MILK-1L",
                        "title", "有机鲜牛奶 1L",
                        "quantity", 1,
                        "unitPrice", Map.of("amount", 1990, "currency", "CNY"))),
                "fulfillments", List.of(Map.of(
                        "type", "STORE_PICKUP",
                        "status", "READY_FOR_PICKUP",
                        "pickupCode", "654321")));
        return new DemoCheckoutResult(
                scenario,
                true,
                order,
                "Demo data only. No order, payment, inventory or message is written.");
    }

    @GetMapping("/pickups/{pickupCode}")
    public PickupDetail pickup(@PathVariable String pickupCode) {
        if (!"654321".equals(pickupCode)) {
            throw ApiException.notFound("PICKUP_NOT_FOUND", "Demo pickup code not found: " + pickupCode);
        }
        return new PickupDetail(
                "654321",
                "READY",
                "上海静安店",
                "DEMO-20261018-0001",
                "演示顾客",
                "13800000000",
                new Money(3980, "CNY"),
                3,
                0,
                3,
                Instant.now().plus(Duration.ofHours(48)),
                List.of(
                        new PickupDetail.PickupItem("有机鲜牛奶", 1),
                        new PickupDetail.PickupItem("手工陶瓷马克杯", 1)));
    }
}