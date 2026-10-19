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
    public Reward accrue(UUID orderId, String phone, long paidAmount) {
        if (phone == null || phone.isBlank() || paidAmount <= 0) {
            return new Reward(0, 0, null);
        }
        List<MemberRow> members = jdbcTemplate.query("""
                SELECT m.id, m.tier, m.points_balance, m.growth_value, l.points_multiplier
                  FROM member m JOIN member_level l ON l.code = m.tier
                 WHERE m.phone = ?
                """, (rs, rowNum) -> new MemberRow(
                rs.getObject("id", UUID.class), rs.getString("tier"),
                rs.getLong("points_balance"), rs.getLong("growth_value"),
                rs.getInt("points_multiplier")), phone.trim());
        if (members.isEmpty()) {
            return new Reward(0, 0, null);
        }
        MemberRow member = members.get(0);
        long base = paidAmount / 100;
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
                jdbcTemplate.update("UPDATE member SET points_balance = points_balance + ? WHERE id = ?",
                        points, member.id());
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
                jdbcTemplate.update("UPDATE member SET growth_value = growth_value + ? WHERE id = ?",
                        growth, member.id());
            }
        }

        String newTier = upgradeIfEligible(member.id());
        return new Reward(points, growth, newTier == null ? member.tier() : newTier);
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
