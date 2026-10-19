package com.fullmall.pos.api;

import com.fullmall.platform.money.Money;
import com.fullmall.pos.domain.PosShift;

public record PosShiftView(
        PosShift shift,
        Money cashSales,
        Money cashRefunds,
        Money cashIn,
        Money cashOut,
        Money expectedCash,
        int saleCount
) {
}
