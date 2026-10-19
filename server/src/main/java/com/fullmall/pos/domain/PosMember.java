package com.fullmall.pos.domain;

import com.fullmall.platform.money.Money;

import java.util.UUID;

public record PosMember(
        UUID id,
        String memberNo,
        String name,
        String phone,
        String tier,
        java.time.LocalDate birthday,
        long growthValue,
        Integer birthdayGiftYear,
        long points,
        Money storedValue
) {
}
