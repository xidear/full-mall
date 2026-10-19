package com.fullmall.pos.domain;

public record MemberLevel(
        String code,
        String name,
        int discountRate,
        int pointsMultiplier,
        boolean freeShipping,
        long birthdayPoints,
        long minGrowth,
        int sortOrder
) {
}
