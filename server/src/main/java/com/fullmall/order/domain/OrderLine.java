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
        String lineType
) {
    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}