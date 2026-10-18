package com.fullmall.fulfillment.domain;

import java.time.LocalDate;
import java.util.UUID;

public record PickTaskLine(
        UUID id,
        UUID pickTaskId,
        UUID skuId,
        String title,
        int quantity,
        UUID suggestedBatchId,
        String suggestedBatchNo,
        LocalDate suggestedExpiryDate,
        int pickedQuantity
) {
}