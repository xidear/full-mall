package com.fullmall.pos.api;

import com.fullmall.platform.money.Money;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PosDailyReport(
        LocalDate businessDate,
        UUID storeId,
        String storeName,
        int salesCount,
        int refundCount,
        Money grossSales,
        Money discountTotal,
        Money netSales,
        Money refundTotal,
        Money cashTotal,
        Money pointsDiscount,
        Money couponDiscount,
        long pointsRedeemed,
        long pointsEarned,
        long couponsUsed,
        Money cashVariance,
        int shiftCount,
        List<PosTenderTotal> tenderMix,
        List<PosShiftView> shifts
) {
}
