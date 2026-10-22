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
    /** 排序白名单：只接受已知列，方向仅 asc/desc，未知字段回落最近下单时间。 */
    private static String orderByOf(String sort, String direction) {
        String column = switch (sort == null ? "" : sort.trim()) {
            case "orderCount" -> "order_count";
            case "totalSpent" -> "total_spent";
            default -> "last_order_at";
        };
        String dir = "asc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "ASC" : "DESC";
        return column + " " + dir;
    }

    public Map<String, Object> list(int page, int size) {
        return list(page, size, "lastOrderAt", "desc");
    }

    public Map<String, Object> list(int page, int size, String sort, String direction) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT contact_phone) FROM orders", Long.class);
        List<CustomerSummary> items = jdbcTemplate.query("""
                SELECT contact_phone, MAX(contact_name) AS contact_name, COUNT(*) AS order_count,
                       COALESCE(SUM(total_amount), 0) AS total_spent, MAX(created_at) AS last_order_at
                FROM orders
                GROUP BY contact_phone
                ORDER BY
                """ + orderByOf(sort, direction) + """
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

    /**
     * 客户列表 keyset 分页：按 (最近下单时间, 手机号) 倒序。
     *
     * <p>客户是按手机号聚合出来的，游标只能建在聚合结果上，所以用子查询再过滤。
     */
    public Map<String, Object> listKeyset(String cursorLastOrderAt, String cursorPhone, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        java.time.Instant cursor = null;
        if (cursorLastOrderAt != null && !cursorLastOrderAt.isBlank()) {
            cursor = java.time.Instant.parse(cursorLastOrderAt.trim());
        }
        String phoneCursor = cursorPhone == null || cursorPhone.isBlank() ? null : cursorPhone.trim();
        List<CustomerSummary> items = jdbcTemplate.query("""
                SELECT contact_phone, contact_name, order_count, total_spent, last_order_at
                  FROM (
                        SELECT contact_phone,
                               MAX(contact_name) AS contact_name,
                               COUNT(*) AS order_count,
                               COALESCE(SUM(total_amount), 0) AS total_spent,
                               MAX(created_at) AS last_order_at
                          FROM orders
                         GROUP BY contact_phone
                       ) c
                 WHERE (?::timestamptz IS NULL
                        OR (c.last_order_at, c.contact_phone) < (?::timestamptz, ?::text))
                 ORDER BY c.last_order_at DESC, c.contact_phone DESC
                 LIMIT ?
                """, (rs, rowNum) -> new CustomerSummary(
                rs.getString("contact_phone"),
                rs.getString("contact_name"),
                rs.getLong("order_count"),
                new Money(rs.getLong("total_spent"), "CNY"),
                rs.getTimestamp("last_order_at").toInstant()),
                cursor == null ? null : java.sql.Timestamp.from(cursor),
                cursor == null ? null : java.sql.Timestamp.from(cursor),
                phoneCursor, safeSize);
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("items", items);
        result.put("size", safeSize);
        if (items.isEmpty()) {
            result.put("nextLastOrderAt", null);
            result.put("nextPhone", null);
        } else {
            var last = items.get(items.size() - 1);
            result.put("nextLastOrderAt", last.lastOrderAt().toString());
            result.put("nextPhone", last.phone());
        }
        return result;
    }
}