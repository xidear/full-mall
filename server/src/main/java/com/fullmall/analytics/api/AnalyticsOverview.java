package com.fullmall.analytics.api;

import com.fullmall.platform.money.Money;

public record AnalyticsOverview(
        Money gmv,
        long orders,
        long paidOrders,
        Money aov,
        Double pickupRate,
        Double fulfillmentOnTimeRate,
        Money nearExpiryValue,
        Double botContainmentRate
) {
}