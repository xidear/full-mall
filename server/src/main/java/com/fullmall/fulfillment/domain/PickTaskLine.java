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
        int pickedQuantity,
        /** 唯一来源订单行；多行合并时为空（无法唯一定位）。 */
        UUID orderLineId
) {
}