package com.fullmall.pos.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PosCalculatorTest {

    @Test
    void fixedCouponNeverExceedsDiscountedBase() {
        assertEquals(1000, PosCalculator.couponDiscount(CouponDiscountType.FIXED, 1000, null, 8000));
        assertEquals(500, PosCalculator.couponDiscount(CouponDiscountType.FIXED, 1000, null, 500));
    }

    @Test
    void percentCouponRespectsCap() {
        assertEquals(2000, PosCalculator.couponDiscount(CouponDiscountType.PERCENT, 20, null, 10000));
        assertEquals(1500, PosCalculator.couponDiscount(CouponDiscountType.PERCENT, 20, 1500L, 10000));
    }

    @Test
    void combinedBenefitsReducePayableWithoutFloatingPoint() {
        long coupon = PosCalculator.couponDiscount(CouponDiscountType.FIXED, 1000, null, 3980);
        long payable = PosCalculator.payable(3980, 0, coupon, 0, 500);
        assertEquals(2480, payable);
        assertEquals(24, PosCalculator.pointsEarned(payable));
    }

    @Test
    void pointsCannotExceedBalanceOrPayable() {
        assertEquals(500, PosCalculator.redeemablePoints(500, 1000));
        assertEquals(120, PosCalculator.redeemablePoints(500, 120));
        assertEquals(0, PosCalculator.redeemablePoints(500, 0));
    }

    @Test
    void refundProrationNeverDriftsOnTheLastUnit() {
        long lineNet = PosCalculator.allocateNet(3333, 10000, 10000);
        assertEquals(3333, lineNet);
        assertEquals(1111, PosCalculator.prorated(lineNet, 1, 3));
        assertEquals(2222, PosCalculator.prorated(lineNet, 2, 3));
        assertEquals(3333, PosCalculator.prorated(lineNet, 3, 3));
    }

    @Test
    void netAllocationSumsExactlyToPayable() {
        long[] nets = PosCalculator.allocateNet(new long[]{7960, 3990}, 10850);
        assertEquals(10850, nets[0] + nets[1]);
        assertEquals(7227, nets[0]);
        assertEquals(3623, nets[1]);
    }

    @Test
    void discountsAboveSubtotalFailExplicitly() {
        assertThrows(IllegalArgumentException.class,
                () -> PosCalculator.payable(1000, 300, 400, 300, 100));
    }
}
