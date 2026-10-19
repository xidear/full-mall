package com.fullmall.pos.api;

import com.fullmall.platform.money.Money;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PosReconciliationReport(
        LocalDate businessDate,
        UUID storeId,
        String storeName,
        List<Row> rows
) {
    public record Row(
            String tenderType,
            Money expectedAmount,
            Money actualAmount,
            Money variance,
            String status
    ) {
    }
}
