package com.fullmall.pos.api;

import com.fullmall.platform.money.Money;

public record PosTenderTotal(
        String tenderType,
        Money amount
) {
}
