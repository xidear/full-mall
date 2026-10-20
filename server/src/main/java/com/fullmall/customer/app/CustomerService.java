package com.fullmall.customer.app;

import com.fullmall.customer.api.CustomerSummary;
import com.fullmall.platform.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CustomerService {

    private final JdbcTemplate jdbcTemplate;

    public CustomerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 分页列出客户（按手机号聚合）。
     *
     * <p>返回统一契约 `{items, page, size, total}`，与后台其它列表一致，
     * 便于前端套用同一套分页组件。
     */
    public Map<String, Object> list(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT contact_phone) FROM orders", Long.class);
        List<CustomerSummary> items = jdbcTemplate.query("""
                SELECT contact_phone, MAX(contact_name) AS contact_name, COUNT(*) AS order_count,
                       COALESCE(SUM(total_amount), 0) AS total_spent, MAX(created_at) AS last_order_at
                FROM orders
                GROUP BY contact_phone
                ORDER BY last_order_at DESC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> new CustomerSummary(
                rs.getString("contact_phone"),
                rs.getString("contact_name"),
                rs.getLong("order_count"),
                new Money(rs.getLong("total_spent"), "CNY"),
                rs.getTimestamp("last_order_at").toInstant()), safeSize, safePage * safeSize);

        return Map.of("items", items, "page", safePage, "size", safeSize,
                "total", total == null ? 0L : total);
    }
}