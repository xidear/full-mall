package com.fullmall.order.app;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 按手机号查会员等级折扣率。
 *
 * <p>只返回**折扣率**（如 95 表示 95 折）；查不到会员返回 null（匿名结算，不享受权益）。
 * 不缓存：会员可能刚升级，宁可多查一次也不要用过期折扣。
 */
@Component
public class MemberDiscountLookup {

    private final JdbcTemplate jdbcTemplate;

    public MemberDiscountLookup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Integer discountRate(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        List<Integer> rates = jdbcTemplate.queryForList("""
                SELECT l.discount_rate FROM member m
                  JOIN member_level l ON l.code = m.tier
                 WHERE m.phone = ?
                """, Integer.class, phone.trim());
        return rates.stream().findFirst().orElse(null);
    }
}
