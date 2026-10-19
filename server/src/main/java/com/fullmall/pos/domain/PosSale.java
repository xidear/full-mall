package com.fullmall.pos.domain;

import com.fullmall.platform.money.Money;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PosSale(
        UUID id,
        String saleNo,
        UUID storeId,
        UUID shiftId,
        UUID memberId,
        String currency,
        Money subtotal,
        Money promotionDiscount,
        Money couponDiscount,
        Money memberDiscount,
        Money pointsDiscount,
        Money payable,
        Money cashTendered,
        Money change,
        PosSaleStatus status,
        UUID couponId,
        String couponCode,
        long pointsRedeemed,
        long pointsEarned,
        long pointsReversed,
        Money refunded,
        String operatorName,
        Instant createdAt,
        Instant paidAt,
        List<PosSaleLine> lines,
        List<PosTender> tenders
) {
}
