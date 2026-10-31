package com.fullmall.member.app;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** 会员积分与成长值（MEM-002）：账本 + 幂等 business_key + 只升不降的等级升级。 */
@Service
public class MemberRewardService {

    private final JdbcTemplate jdbcTemplate;

    public MemberRewardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Reward(long points, long growth, String tier) {
    }

    @Transactional
    public Reward accrue(UUID orderId, String phone, long paidAmount, String currency) {
        if (phone == null || phone.isBlank() || paidAmount <= 0) {
            return new Reward(0, 0, null);
        }
        List<MemberRow> members = jdbcTemplate.query("""
                SELECT m.id, m.tier, m.points_balance, m.growth_value, l.points_multiplier
                  FROM member m JOIN member_level l ON l.code = m.tier
                 WHERE m.phone = ? FOR UPDATE
                """, (rs, rowNum) -> new MemberRow(
                rs.getObject("id", UUID.class), rs.getString("tier"),
                rs.getLong("points_balance"), rs.getLong("growth_value"),
                rs.getInt("points_multiplier")), phone.trim());
        if (members.isEmpty()) {
            return new Reward(0, 0, null);
        }
        MemberRow member = members.get(0);
        int units = com.fullmall.platform.money.CurrencyPrecision.minorUnits(currency);
        long base = paidAmount / (long) Math.pow(10, units);
        long points = base * member.multiplier();

        if (points > 0) {
            int inserted = jdbcTemplate.update("""
                    INSERT INTO points_ledger (id, member_id, sale_id, entry_type, points,
                                               balance_after, reason, business_key)
                    VALUES (?, ?, NULL, 'EARN', ?, ?, ?, ?)
                    ON CONFLICT (business_key) DO NOTHING
                    """, UUID.randomUUID(), member.id(), points,
                    member.points() + points, "订单消费积分", "order:" + orderId + ":points");
            if (inserted == 1) {
                int updated = jdbcTemplate.update(
                        "UPDATE member SET points_balance = points_balance + ? WHERE id = ?",
                        points, member.id());
                if (updated != 1) {
                    throw new IllegalStateException("会员积分入账失败：member=" + member.id());
                }
            }
        }

        long growth = base;
        if (growth > 0) {
            int inserted = jdbcTemplate.update("""
                    INSERT INTO growth_ledger (id, member_id, sale_id, entry_type, growth,
                                               balance_after, reason, business_key)
                    VALUES (?, ?, NULL, 'EARN', ?, ?, ?, ?)
                    ON CONFLICT (business_key) DO NOTHING
                    """, UUID.randomUUID(), member.id(), growth,
                    member.growth() + growth, "订单成长值", "order:" + orderId + ":growth");
            if (inserted == 1) {
                int updated = jdbcTemplate.update(
                        "UPDATE member SET growth_value = growth_value + ? WHERE id = ?",
                        growth, member.id());
                if (updated != 1) {
                    throw new IllegalStateException("会员成长值入账失败：member=" + member.id());
                }
            }
        }

        String newTier = upgradeIfEligible(member.id());
        return new Reward(points, growth, newTier == null ? member.tier() : newTier);
    }

    /**
     * 退款/取消：回退该订单赚取的积分与成长值（幂等）。
     *
     * <p>只回退**当前仍持有**的部分（余额不足时不产生负数），并写 REVERSE 流水，
     * 便于事后查"本该退多少、实际退了多少"。
     */
    @Transactional
    public Reward rollback(UUID orderId, String phone) {
        if (phone == null || phone.isBlank()) {
            return new Reward(0, 0, null);
        }
        List<MemberRow> members = jdbcTemplate.query("""
                SELECT m.id, m.tier, m.points_balance, m.growth_value, l.points_multiplier
                  FROM member m JOIN member_level l ON l.code = m.tier
                 WHERE m.phone = ? FOR UPDATE
                """, (rs, rowNum) -> new MemberRow(
                rs.getObject("id", UUID.class), rs.getString("tier"),
                rs.getLong("points_balance"), rs.getLong("growth_value"),
                rs.getInt("points_multiplier")), phone.trim());
        if (members.isEmpty()) {
            return new Reward(0, 0, null);
        }
        MemberRow member = members.get(0);

        long earnedPoints = earned("points_ledger", "points", orderId);
        long earnedGrowth = earned("growth_ledger", "growth", orderId);
        long reversePoints = Math.min(earnedPoints, member.points());
        long reverseGrowth = Math.min(earnedGrowth, member.growth());

        if (reversePoints > 0) {
            int inserted = jdbcTemplate.update("""
                    INSERT INTO points_ledger (id, member_id, sale_id, entry_type, points,
                                               balance_after, reason, business_key)
                    VALUES (?, ?, NULL, 'REVERSE', ?, ?, ?, ?)
                    ON CONFLICT (business_key) DO NOTHING
                    """, UUID.randomUUID(), member.id(), -reversePoints,
                    member.points() - reversePoints, "订单退款回退积分", "order:" + orderId + ":points-rollback");
            if (inserted == 1) {
                int updated = jdbcTemplate.update(
                        "UPDATE member SET points_balance = points_balance - ? WHERE id = ? AND points_balance >= ?",
                        reversePoints, member.id(), reversePoints);
                if (updated != 1) {
                    throw new IllegalStateException("会员积分回退并发冲突：member=" + member.id());
                }
            }
        }
        if (reverseGrowth > 0) {
            int inserted = jdbcTemplate.update("""
                    INSERT INTO growth_ledger (id, member_id, sale_id, entry_type, growth,
                                               balance_after, reason, business_key)
                    VALUES (?, ?, NULL, 'REVERSE', ?, ?, ?, ?)
                    ON CONFLICT (business_key) DO NOTHING
                    """, UUID.randomUUID(), member.id(), -reverseGrowth,
                    member.growth() - reverseGrowth, "订单退款回退成长值",
                    "order:" + orderId + ":growth-rollback");
            if (inserted == 1) {
                int updated = jdbcTemplate.update(
                        "UPDATE member SET growth_value = growth_value - ? WHERE id = ? AND growth_value >= ?",
                        reverseGrowth, member.id(), reverseGrowth);
                if (updated != 1) {
                    throw new IllegalStateException("会员成长值回退并发冲突：member=" + member.id());
                }
            }
        }
        return new Reward(reversePoints, reverseGrowth, member.tier());
    }

    /**
     * 部分退款按比例回退赚取的积分/成长值（**可选策略**，默认不启用）。
     *
     * <p>比例 = 本次退款金额 / 订单实付金额，**向下取整**（不多退）；仍受"不产生负数"保护。
     * 是否启用由 `app.refund.partial.points-mode` 决定：`KEEP`（默认，现状）或 `PROPORTIONAL`。
     */
    @Transactional
    public Reward rollbackProportional(UUID orderId, String phone, long refundedAmount, long orderTotal) {
        if (orderTotal <= 0 || refundedAmount <= 0) {
            throw new IllegalArgumentException(
                    "按比例回退需要正的退款额与订单额：refunded=" + refundedAmount + ", total=" + orderTotal);
        }
        if (refundedAmount > orderTotal) {
            throw new IllegalArgumentException(
                    "退款额不能超过订单额：refunded=" + refundedAmount + ", total=" + orderTotal);
        }
        List<MemberRow> members = jdbcTemplate.query("""
                SELECT m.id, m.tier, m.points_balance, m.growth_value, l.points_multiplier
                  FROM member m JOIN member_level l ON l.code = m.tier
                 WHERE m.phone = ? FOR UPDATE
                """, (rs, rowNum) -> new MemberRow(
                rs.getObject("id", UUID.class), rs.getString("tier"),
                rs.getLong("points_balance"), rs.getLong("growth_value"),
                rs.getInt("points_multiplier")), phone.trim());
        if (members.isEmpty()) {
            return new Reward(0, 0, null);
        }
        MemberRow member = members.get(0);

        long earnedPoints = earned("points_ledger", "points", orderId);
        long earnedGrowth = earned("growth_ledger", "growth", orderId);
        long proportionalPoints = earnedPoints * refundedAmount / orderTotal;
        long proportionalGrowth = earnedGrowth * refundedAmount / orderTotal;
        long reversePoints = Math.min(proportionalPoints, member.points());
        long reverseGrowth = Math.min(proportionalGrowth, member.growth());

        if (reversePoints > 0) {
            int inserted = jdbcTemplate.update("""
                    INSERT INTO points_ledger (id, member_id, sale_id, entry_type, points,
                                               balance_after, reason, business_key)
                    VALUES (?, ?, NULL, 'REVERSE', ?, ?, ?, ?)
                    ON CONFLICT (business_key) DO NOTHING
                    """, UUID.randomUUID(), member.id(), -reversePoints,
                    member.points() - reversePoints, "部分退款按比例回退积分",
                    "order:" + orderId + ":points-partial:" + refundedAmount);
            if (inserted == 1) {
                int updated = jdbcTemplate.update(
                        "UPDATE member SET points_balance = points_balance - ? WHERE id = ? AND points_balance >= ?",
                        reversePoints, member.id(), reversePoints);
                if (updated != 1) {
                    throw new IllegalStateException("会员积分回退并发冲突：member=" + member.id());
                }
            }
        }
        if (reverseGrowth > 0) {
            int inserted = jdbcTemplate.update("""
                    INSERT INTO growth_ledger (id, member_id, sale_id, entry_type, growth,
                                               balance_after, reason, business_key)
                    VALUES (?, ?, NULL, 'REVERSE', ?, ?, ?, ?)
                    ON CONFLICT (business_key) DO NOTHING
                    """, UUID.randomUUID(), member.id(), -reverseGrowth,
                    member.growth() - reverseGrowth, "部分退款按比例回退成长值",
                    "order:" + orderId + ":growth-partial:" + refundedAmount);
            if (inserted == 1) {
                int updated = jdbcTemplate.update(
                        "UPDATE member SET growth_value = growth_value - ? WHERE id = ? AND growth_value >= ?",
                        reverseGrowth, member.id(), reverseGrowth);
                if (updated != 1) {
                    throw new IllegalStateException("会员成长值回退并发冲突：member=" + member.id());
                }
            }
        }
        return new Reward(reversePoints, reverseGrowth, member.tier());
    }

    private long earned(String table, String column, UUID orderId) {
        List<Long> rows = jdbcTemplate.queryForList(
                "SELECT " + column + " FROM " + table + " WHERE business_key = ?",
                Long.class, "order:" + orderId + ":" + (table.startsWith("points") ? "points" : "growth"));
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    private String upgradeIfEligible(UUID memberId) {
        List<String> tiers = jdbcTemplate.queryForList("""
                SELECT l.code FROM member_level l
                 WHERE l.min_growth <= (SELECT growth_value FROM member WHERE id = ?)
                 ORDER BY l.min_growth DESC
                 LIMIT 1
                """, String.class, memberId);
        if (tiers.isEmpty()) {
            return null;
        }
        String target = tiers.get(0);
        int updated = jdbcTemplate.update("""
                UPDATE member m SET tier = ?
                 WHERE m.id = ?
                   AND (SELECT sort_order FROM member_level WHERE code = ?)
                     > (SELECT sort_order FROM member_level WHERE code = m.tier)
                """, target, memberId, target);
        return updated == 1 ? target : null;
    }

    private record MemberRow(UUID id, String tier, long points, long growth, int multiplier) {
    }
}
