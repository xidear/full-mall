package com.fullmall.analytics.infra;

import com.fullmall.analytics.api.AnalyticsOverview;
import com.fullmall.platform.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class AnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String eventType, UUID orderId, UUID skuId, String propertiesJson) {
        jdbcTemplate.update("""
                INSERT INTO analytics_event (id, event_type, occurred_at, market_code, channel,
                                             order_id, sku_id, properties_json, schema_version)
                VALUES (?, ?, now(), 'CN', 'WEB', ?, ?, ?::jsonb, 1)
                """, UUID.randomUUID(), eventType, orderId, skuId, propertiesJson);
    }

    public AnalyticsOverview overview() {
        SalesSummary sales = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(total_amount), 0) AS gmv,
                       COUNT(*) AS orders,
                       COUNT(*) FILTER (WHERE status IN ('PAID', 'ALLOCATED', 'PARTIALLY_FULFILLED', 'FULFILLED')) AS paid_orders
                FROM orders
                """, (rs, rowNum) -> new SalesSummary(
                rs.getLong("gmv"), rs.getLong("orders"), rs.getLong("paid_orders")));
        if (sales == null) {
            throw new IllegalStateException("Sales summary query returned null");
        }

        FulfillmentSummary fulfillment = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE type = 'STORE_PICKUP') AS pickup_total
                FROM fulfillment_order
                """, (rs, rowNum) -> new FulfillmentSummary(
                rs.getLong("total"), rs.getLong("pickup_total")));
        if (fulfillment == null) {
            throw new IllegalStateException("Fulfillment summary query returned null");
        }

        Long nearExpiryValue = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity * COALESCE(unit_cost_amount, 0)), 0)
                FROM inventory_batch
                WHERE expiry_date IS NOT NULL
                  AND expiry_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days'
                  AND status IN ('AVAILABLE', 'NEAR_EXPIRY')
                """, Long.class);
        if (nearExpiryValue == null) {
            throw new IllegalStateException("Near expiry value query returned null");
        }

        Money gmv = new Money(sales.gmv(), "CNY");
        Money aov = sales.paidOrders() == 0 ? null : new Money(sales.gmv() / sales.paidOrders(), "CNY");
        Double pickupRate = fulfillment.total() == 0
                ? null
                : (double) fulfillment.pickupTotal() / fulfillment.total();
        return new AnalyticsOverview(
                gmv,
                sales.orders(),
                sales.paidOrders(),
                aov,
                pickupRate,
                null,
                new Money(nearExpiryValue, "CNY"),
                null);
    }

    private record SalesSummary(long gmv, long orders, long paidOrders) {
    }

    private record FulfillmentSummary(long total, long pickupTotal) {
    }
}