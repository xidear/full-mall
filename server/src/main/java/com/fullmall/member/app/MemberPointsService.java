package com.fullmall.member.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 积分抵扣与回退（MEM-003）。
 *
 * <p>兑换比例：**1 积分 = 1 分（最小货币单位）**，即"100 积分抵 1 元"，
 * 通过配置项 `app.member.points.per-minor-unit` 表达（默认 1）。
 *
 * <p>两条硬规则：
 * <ul>
 *   <li>只能抵扣应付金额（不允许抵成负数）；</li>
 *   <li>所有变动走 `points_ledger` + `business_key` 幂等，重复调用不重复扣/加。</li>
 * </ul>
 */
@Service
public class MemberPointsService {

    private final JdbcTemplate jdbcTemplate;
    private final long pointsPerMinorUnit;

    public MemberPointsService(JdbcTemplate jdbcTemplate,
                               @org.springframework.beans.factory.annotation.Value(
                                       "${app.member.points.per-minor-unit:1}") long pointsPerMinorUnit) {
        if (pointsPerMinorUnit <= 0) {
            throw new IllegalArgumentException("积分兑换比例必须大于 0，实际=" + pointsPerMinorUnit);
        }
        this.pointsPerMinorUnit = pointsPerMinorUnit;
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Redemption(long pointsUsed, long amount, long balanceAfter) {
    }

    @Transactional
    public Redemption redeem(String phone, UUID orderId, long requestedPoints, long payableAmount) {
        if (phone == null || phone.isBlank() || requestedPoints <= 0) {
            return new Redemption(0, 0, 0);
        }
        MemberBalance member = requireMember(phone);
        if (requestedPoints > member.points()) {
            throw ApiException.conflict("INSUFFICIENT_POINTS",
                    "积分不足：可用 " + member.points() + "，请求 " + requestedPoints);
        }
        long maxPoints = payableAmount * pointsPerMinorUnit;
        long used = Math.min(requestedPoints, maxPoints);
        if (used <= 0) {
            return new Redemption(0, 0, member.points());
        }
        long amount = used / pointsPerMinorUnit;
        if (amount <= 0) {
            return new Redemption(0, 0, member.points());
        }

        int inserted = jdbcTemplate.update("""
                INSERT INTO points_ledger (id, member_id, sale_id, entry_type, points,
                                           balance_after, reason, business_key)
                VALUES (?, ?, NULL, 'REDEEM', ?, ?, ?, ?)
                ON CONFLICT (business_key) DO NOTHING
                """, UUID.randomUUID(), member.id(), -used, member.points() - used,
                "订单积分抵扣", "order:" + orderId + ":redeem");
        if (inserted != 1) {
            throw ApiException.conflict("POINTS_ALREADY_REDEEMED",
                    "该订单已使用过积分抵扣: " + orderId);
        }
        jdbcTemplate.update("UPDATE member SET points_balance = points_balance - ? WHERE id = ?",
                used, member.id());
        return new Redemption(used, amount, member.points() - used);
    }

    /** 退款/取消：把该订单抵扣的积分退回（幂等）。 */
    @Transactional
    public long rollbackForOrder(UUID orderId, String phone) {
        if (phone == null || phone.isBlank()) {
            return 0;
        }
        List<Long> redeems = jdbcTemplate.queryForList("""
                SELECT points FROM points_ledger
                 WHERE business_key = ? AND entry_type = 'REDEEM'
                """, Long.class, "order:" + orderId + ":redeem");
        if (redeems.isEmpty()) {
            return 0;
        }
        long used = -redeems.get(0);
        MemberBalance member = requireMember(phone);
        int inserted = jdbcTemplate.update("""
                INSERT INTO points_ledger (id, member_id, sale_id, entry_type, points,
                                           balance_after, reason, business_key)
                VALUES (?, ?, NULL, 'REFUND', ?, ?, ?, ?)
                ON CONFLICT (business_key) DO NOTHING
                """, UUID.randomUUID(), member.id(), used, member.points() + used,
                "订单退款退回积分", "order:" + orderId + ":redeem-rollback");
        if (inserted != 1) {
            return 0;
        }
        jdbcTemplate.update("UPDATE member SET points_balance = points_balance + ? WHERE id = ?",
                used, member.id());
        return used;
    }

    private MemberBalance requireMember(String phone) {
        return jdbcTemplate.query("""
                SELECT id, points_balance FROM member WHERE phone = ?
                """, (rs, rowNum) -> new MemberBalance(
                rs.getObject("id", UUID.class), rs.getLong("points_balance")), phone.trim())
                .stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("MEMBER_NOT_FOUND", "会员不存在: " + phone));
    }

    private record MemberBalance(UUID id, long points) {
    }
}
