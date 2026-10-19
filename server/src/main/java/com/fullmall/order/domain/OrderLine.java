package com.fullmall.order.domain;

import com.fullmall.platform.domain.FulfillmentType;
import com.fullmall.platform.money.Money;

import java.util.UUID;

public record OrderLine(
        UUID id,
        UUID skuId,
        String title,
        String skuCode,
        int quantity,
        Money unitPrice,
        FulfillmentType fulfillmentType,
        UUID promotionId,
        String lineType,
        String snapshot
) {
    /**
     * 规格快照文本（如 "颜色:红;尺码:M"）。用稳定顺序，便于对账与人工核对；
     * 不是 JSON——后台不录 JSON，快照也不存 JSON。
     */
    public static String attributesText(java.util.Map<String, String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        return new java.util.TreeMap<>(options).entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(java.util.stream.Collectors.joining(";"));
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}