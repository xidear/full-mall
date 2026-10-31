package com.fullmall.order.domain;

import com.fullmall.platform.money.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(
        UUID id,
        String orderNo,
        OrderStatus status,
        String currency,
        /** 下单时的市场快照（ADR-0014）：报表、税务归档与对账都要按市场切分。 */
        String marketCode,
        Money subtotal,
        Money shippingFee,
        /** 下单时按 SKU 净重汇总的重量快照（报关与运费复算用）；缺失重量的商品按 0 计。 */
        Integer totalWeightGrams,
        Money discount,
        Money total,
        String contactName,
        String contactPhone,
        String contactEmail,
        String addressJson,
        FulfillmentMode fulfillmentMode,
        UUID storeId,
        Instant createdAt,
        List<OrderLine> lines,
        /** 下单时 订单币种→基准币种 的汇率快照；无可用汇率时为空（不阻塞下单）。 */
        BigDecimal fxRate,
        String fxBaseCurrency,
        Long fxBaseTotalAmount,
        Instant fxRateAt
) {
    public Order {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}