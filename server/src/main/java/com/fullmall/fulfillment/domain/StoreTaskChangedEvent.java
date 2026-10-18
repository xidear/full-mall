package com.fullmall.fulfillment.domain;

import java.util.UUID;

public record StoreTaskChangedEvent(UUID storeId, UUID taskId, String changeType) {
}