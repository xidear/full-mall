package com.fullmall.pos.api;

import com.fullmall.pos.domain.PosCoupon;
import com.fullmall.pos.domain.PosMember;

import java.util.List;

public record PosMemberView(
        PosMember member,
        List<PosCoupon> coupons
) {
}
