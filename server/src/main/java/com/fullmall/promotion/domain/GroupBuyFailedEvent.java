package com.fullmall.promotion.domain;

import java.util.List;
import java.util.UUID;

public record GroupBuyFailedEvent(UUID groupBuyId, List<UUID> paidOrderIds) {
}
