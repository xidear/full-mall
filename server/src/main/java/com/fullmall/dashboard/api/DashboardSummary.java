package com.fullmall.dashboard.api;

import com.fullmall.platform.money.Money;

import java.util.List;

public record DashboardSummary(
        long todayOrders,
        long todayPaidOrders,
        Money todaySales,
        long pendingPayment,
        long pendingFulfillment,
        long pendingPickup,
        long lowStock,
        long nearExpiry,
        long activeProducts,
        long customers,
        List<TodoItem> todos
) {
    public record TodoItem(String code, String label, long count, String path) {
    }
}