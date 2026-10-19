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
import com.fullmall.pos.api.PosSaleView;
import com.fullmall.pos.api.PosShiftView;
import com.fullmall.pos.domain.PosMember;
import com.fullmall.pos.domain.PosSale;
import com.fullmall.pos.domain.PosSaleLine;
import com.fullmall.pos.domain.PosSaleStatus;
import com.fullmall.pos.domain.PosShift;
import com.fullmall.pos.domain.PosShiftStatus;
import com.fullmall.pos.domain.PosTender;
import com.fullmall.pos.domain.TenderStatus;
import com.fullmall.pos.domain.TenderType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @GetMapping("/pos/shift")
    public PosShiftView posShift() {
        PosShift shift = new PosShift(
                UUID.fromString("f0000000-0000-0000-0000-000000000001"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                null,
                "演示收银员",
                PosShiftStatus.OPEN,
                new Money(20000, "CNY"),
                new Money(23980, "CNY"),
                null,
                null,
                null,
                Instant.now().minus(Duration.ofHours(2)),
                null);
        return new PosShiftView(
                shift,
                new Money(3980, "CNY"),
                Money.zero("CNY"),
                Money.zero("CNY"),
                Money.zero("CNY"),
                new Money(23980, "CNY"),
                1);
    }

    @GetMapping("/pos/sale")
    public PosSaleView posSale() {
        Instant now = Instant.now().minus(Duration.ofMinutes(12));
        PosSaleLine line = new PosSaleLine(
                UUID.fromString("f1000000-0000-0000-0000-000000000001"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                "有机鲜牛奶",
                "MILK-1L",
                2,
                new Money(1990, "CNY"),
                new Money(3980, "CNY"),
                0,
                Money.zero("CNY"),
                "MILK-SH-BULK");
        PosTender tender = new PosTender(
                UUID.fromString("f2000000-0000-0000-0000-000000000001"),
                TenderType.CASH,
                new Money(3980, "CNY"),
                "internal",
                null,
                TenderStatus.SUCCESS,
                new Money(5000, "CNY"),
                new Money(1020, "CNY"),
                Money.zero("CNY"),
                null,
                now);
        PosSale sale = new PosSale(
                UUID.fromString("f3000000-0000-0000-0000-000000000001"),
                "POS-DEMO-0001",
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("f0000000-0000-0000-0000-000000000001"),
                UUID.fromString("a1000000-0000-0000-0000-000000000001"),
                "CNY",
                new Money(3980, "CNY"),
                Money.zero("CNY"),
                Money.zero("CNY"),
                Money.zero("CNY"),
                Money.zero("CNY"),
                new Money(3980, "CNY"),
                new Money(5000, "CNY"),
                new Money(1020, "CNY"),
                PosSaleStatus.COMPLETED,
                null,
                null,
                0,
                39,
                0,
                Money.zero("CNY"),
                "演示收银员",
                now,
                now,
                List.of(line),
                List.of(tender));
        PosMember member = new PosMember(
                UUID.fromString("a1000000-0000-0000-0000-000000000001"),
                "M0001",
                "王小明",
                "13800000001",
                "GOLD",
                null,
                0,
                null,
                500,
                new Money(10000, "CNY"));
        return new PosSaleView(
                sale,
                new Money(3980, "CNY"),
                Money.zero("CNY"),
                member,
                null,
                "上海静安店");
    }
}