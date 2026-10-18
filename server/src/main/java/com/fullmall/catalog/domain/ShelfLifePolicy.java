package com.fullmall.catalog.domain;

public record ShelfLifePolicy(
        boolean batchTracking,
        PickingStrategy pickingStrategy,
        int minRemainingShelfLifeDays,
        int nearExpiryThresholdDays,
        NearExpiryAction nearExpiryAction,
        boolean autoBlockExpired
) {

    public static ShelfLifePolicy none() {
        return new ShelfLifePolicy(false, PickingStrategy.NONE, 0, 0, NearExpiryAction.NORMAL, true);
    }

    public ShelfLifePolicy {
        if (pickingStrategy == null) {
            pickingStrategy = PickingStrategy.NONE;
        }
        if (nearExpiryAction == null) {
            nearExpiryAction = NearExpiryAction.NORMAL;
        }
        if (minRemainingShelfLifeDays < 0) {
            throw new IllegalArgumentException("minRemainingShelfLifeDays must not be negative");
        }
        if (nearExpiryThresholdDays < 0) {
            throw new IllegalArgumentException("nearExpiryThresholdDays must not be negative");
        }
    }
}