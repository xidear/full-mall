package com.fullmall.fulfillment.domain;

import java.util.UUID;

public record FulfillmentLine(
        UUID id,
        UUID fulfillmentOrderId,
        UUID orderLineId,
        UUID skuId,
        int quantity
) {
}