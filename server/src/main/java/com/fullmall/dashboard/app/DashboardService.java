package com.fullmall.dashboard.app;

import com.fullmall.dashboard.api.DashboardSummary;
import com.fullmall.platform.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSummary summary() {
        long todayOrders = count("""
                SELECT COUNT(*) FROM orders WHERE created_at::date = CURRENT_DATE
                """);
        long todayPaidOrders = count("""
                SELECT COUNT(*) FROM orders
                WHERE created_at::date = CURRENT_DATE
                  AND status IN ('PAID', 'ALLOCATED', 'PARTIALLY_FULFILLED', 'FULFILLED')
                """);
        long todaySales = count("""
                SELECT COALESCE(SUM(total_amount), 0) FROM orders
                WHERE created_at::date = CURRENT_DATE
                  AND status IN ('PAID', 'ALLOCATED', 'PARTIALLY_FULFILLED', 'FULFILLED')
                """);
        long pendingPayment = count("""
                SELECT COUNT(*) FROM orders WHERE status = 'PENDING_PAYMENT'
                """);
        long pendingFulfillment = count("""
                SELECT COUNT(*) FROM fulfillment_order
                WHERE status IN ('ALLOCATED', 'PICKING', 'PACKED')
                """);
        long pendingPickup = count("""
                SELECT COUNT(*) FROM pickup WHERE status IN ('READY', 'PARTIALLY_USED')
                """);
        long lowStock = count("""
                SELECT COUNT(*) FROM inventory_batch
                WHERE quantity - reserved_quantity > 0
                  AND quantity - reserved_quantity < 10
                """);
        long nearExpiry = count("""
                SELECT COUNT(*) FROM inventory_batch
                WHERE expiry_date IS NOT NULL
                  AND expiry_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days'
                  AND quantity - reserved_quantity > 0
                """);
        long activeProducts = count("""
                SELECT COUNT(*) FROM product WHERE status = 'ACTIVE'
                """);
        long customers = count("""
                SELECT COUNT(DISTINCT contact_phone) FROM orders
                """);

        List<DashboardSummary.TodoItem> todos = new ArrayList<>();
        todos.add(new DashboardSummary.TodoItem("PENDING_PAYMENT", "待付款订单", pendingPayment, "/orders"));
        todos.add(new DashboardSummary.TodoItem("PENDING_FULFILLMENT", "待发货/待出库", pendingFulfillment, "/orders"));
        todos.add(new DashboardSummary.TodoItem("PENDING_PICKUP", "待自提", pendingPickup, "/orders"));
        todos.add(new DashboardSummary.TodoItem("LOW_STOCK", "低库存预警", lowStock, "/inventory"));
        todos.add(new DashboardSummary.TodoItem("NEAR_EXPIRY", "临期批次", nearExpiry, "/inventory"));

        return new DashboardSummary(
                todayOrders,
                todayPaidOrders,
                new Money(todaySales, "CNY"),
                pendingPayment,
                pendingFulfillment,
                pendingPickup,
                lowStock,
                nearExpiry,
                activeProducts,
                customers,
                todos);
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        if (value == null) {
            throw new IllegalStateException("Dashboard count query returned null: " + sql);
        }
        return value;
    }
}