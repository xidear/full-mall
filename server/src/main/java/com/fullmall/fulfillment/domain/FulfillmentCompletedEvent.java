package com.fullmall.fulfillment.domain;

import java.util.UUID;

public record FulfillmentCompletedEvent(UUID orderId) {
}