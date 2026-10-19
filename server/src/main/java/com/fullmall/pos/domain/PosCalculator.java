package com.fullmall.pos.domain;

/**
 * Pure money rules for POS sales. All amounts are integer minor units; points are 1 point = 1 fen.
 */
public final class PosCalculator {

    private PosCalculator() {
    }

    public static long couponDiscount(CouponDiscountType type, long value, Long maxDiscount,
                                      long discountedBase) {
        if (discountedBase <= 0) {
            return 0;
        }
        long raw = switch (type) {
            case FIXED -> value;
            case PERCENT -> Math.round(discountedBase * (value / 100.0d));
        };
        long capped = maxDiscount == null ? raw : Math.min(raw, maxDiscount);
        return Math.min(Math.max(capped, 0), discountedBase);
    }

    public static long payable(long subtotal, long promotionDiscount, long couponDiscount,
                               long memberDiscount, long pointsDiscount) {
        long payable = subtotal - promotionDiscount - couponDiscount - memberDiscount - pointsDiscount;
        if (payable < 0) {
            throw new IllegalArgumentException("POS discounts exceed subtotal: subtotal=" + subtotal
                    + ", promotion=" + promotionDiscount + ", coupon=" + couponDiscount
                    + ", member=" + memberDiscount + ", points=" + pointsDiscount);
        }
        return payable;
    }

    public static long redeemablePoints(long balance, long payable) {
        return Math.max(Math.min(balance, payable), 0);
    }

    public static long pointsEarned(long payable) {
        return Math.max(payable / 100, 0);
    }

    /** Allocates sale-level discounts to one line using the payable/subtotal ratio. */
    public static long allocateNet(long lineTotal, long payable, long subtotal) {
        if (subtotal <= 0) {
            return 0;
        }
        return (lineTotal * payable + subtotal / 2) / subtotal;
    }

    /**
     * Allocates sale-level discounts across all lines with largest-remainder rounding so the
     * allocated net amounts sum exactly to payable.
     */
    public static long[] allocateNet(long[] lineTotals, long payable) {
        long[] result = new long[lineTotals.length];
        long subtotal = 0;
        for (long lineTotal : lineTotals) {
            subtotal = Math.addExact(subtotal, lineTotal);
        }
        if (subtotal <= 0 || payable <= 0) {
            return result;
        }
        long allocated = 0;
        long[] remainders = new long[lineTotals.length];
        for (int i = 0; i < lineTotals.length; i++) {
            long numerator = Math.multiplyExact(lineTotals[i], payable);
            result[i] = numerator / subtotal;
            remainders[i] = numerator % subtotal;
            allocated = Math.addExact(allocated, result[i]);
        }
        long remaining = payable - allocated;
        while (remaining > 0) {
            int best = -1;
            for (int i = 0; i < remainders.length; i++) {
                if (remainders[i] > 0 && (best < 0 || remainders[i] > remainders[best])) {
                    best = i;
                }
            }
            if (best < 0) {
                throw new IllegalStateException("Unable to allocate all payable amounts: remaining=" + remaining);
            }
            result[best] += 1;
            remainders[best] = 0;
            remaining -= 1;
        }
        return result;
    }

    /** Prorates a line's net amount by the cumulative refunded quantity, rounded to the nearest fen. */
    public static long prorated(long netTotal, int partQuantity, int wholeQuantity) {
        if (wholeQuantity <= 0) {
            throw new IllegalArgumentException("wholeQuantity must be positive: " + wholeQuantity);
        }
        if (partQuantity >= wholeQuantity) {
            return netTotal;
        }
        return (netTotal * partQuantity + wholeQuantity / 2) / wholeQuantity;
    }
}