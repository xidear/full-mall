package com.fullmall.pos.api;

import com.fullmall.platform.money.Money;
import com.fullmall.pos.domain.PosMember;

import java.time.Instant;

public record MemberAdminView(
        PosMember member,
        int saleCount,
        Money totalSpent,
        Instant createdAt
) {
}
