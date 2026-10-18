package com.fullmall.customer.app;

import com.fullmall.customer.api.CustomerSummary;
import com.fullmall.platform.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final JdbcTemplate jdbcTemplate;

    public CustomerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CustomerSummary> list(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return jdbcTemplate.query("""
                SELECT contact_phone, MAX(contact_name) AS contact_name, COUNT(*) AS order_count,
                       COALESCE(SUM(total_amount), 0) AS total_spent, MAX(created_at) AS last_order_at
                FROM orders
                GROUP BY contact_phone
                ORDER BY last_order_at DESC
                LIMIT ?
                """, (rs, rowNum) -> new CustomerSummary(
                rs.getString("contact_phone"),
                rs.getString("contact_name"),
                rs.getLong("order_count"),
                new Money(rs.getLong("total_spent"), "CNY"),
                rs.getTimestamp("last_order_at").toInstant()), safeLimit);
    }
}