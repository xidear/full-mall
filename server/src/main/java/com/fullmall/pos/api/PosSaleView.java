package com.fullmall.pos.api;

import com.fullmall.platform.money.Money;
import com.fullmall.pos.domain.PosCoupon;
import com.fullmall.pos.domain.PosMember;
import com.fullmall.pos.domain.PosSale;

public record PosSaleView(
        PosSale sale,
        Money paidAmount,
        Money remainingAmount,
        PosMember member,
        PosCoupon coupon,
        String storeName
) {
}
