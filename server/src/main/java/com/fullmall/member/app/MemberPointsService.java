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
        // 先锁会员行，再锁 points_ledger：所有积分扣减都按这个顺序，避免并发超扣与死锁。
        MemberBalance member = requireMemberForUpdate(phone);
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
        consumeFifo(member.id(), used);
        int updated = jdbcTemplate.update(
                "UPDATE member SET points_balance = points_balance - ? WHERE id = ? AND points_balance >= ?",
                used, member.id(), used);
        if (updated != 1) {
            throw ApiException.conflict("POINTS_CONCURRENT_MODIFIED",
                    "积分余额被并发修改，请重试：member=" + member.id());
        }
        return new Redemption(used, amount, member.points() - used);
    }

    /**
     * 按 FIFO 从最早的 EARN 行扣减逐笔余额（过期计算的基础）。
     *
     * <p>为什么必须逐笔：只记总余额时无法判断"哪一笔过期了"，过期任务只能按总额猜，会账面不平。
     *
     * <p>余量不足时**抛错**而不是少扣：说明账本与余额已不一致，必须暴露。
     */
    private void consumeFifo(UUID memberId, long points) {
        long remaining = points;
        var rows = jdbcTemplate.query("""
                SELECT id, remaining_points FROM points_ledger
                 WHERE member_id = ? AND entry_type = 'EARN' AND remaining_points > 0
                 ORDER BY created_at, id
                 FOR UPDATE
                """, (rs, rowNum) -> new java.util.AbstractMap.SimpleEntry<>(
                rs.getObject("id", UUID.class), rs.getLong("remaining_points")), memberId);
        for (var row : rows) {
            if (remaining <= 0) {
                break;
            }
            long take = Math.min(row.getValue(), remaining);
            int updated = jdbcTemplate.update(
                    "UPDATE points_ledger SET remaining_points = remaining_points - ?"
                            + " WHERE id = ? AND remaining_points >= ?",
                    take, row.getKey(), take);
            if (updated != 1) {
                throw new IllegalStateException("积分账本并发冲突：ledger=" + row.getKey());
            }
            remaining -= take;
        }
        if (remaining > 0) {
            throw new IllegalStateException("积分账本不一致：余额不足以扣减，缺口 " + remaining
                    + "，会员=" + memberId);
        }
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
        MemberBalance member = requireMemberForUpdate(phone);
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
        int updated = jdbcTemplate.update(
                "UPDATE member SET points_balance = points_balance + ? WHERE id = ?",
                used, member.id());
        if (updated != 1) {
            throw ApiException.conflict("MEMBER_NOT_FOUND", "会员不存在：" + member.id());
        }
        return used;
    }

    private MemberBalance requireMemberForUpdate(String phone) {
        return jdbcTemplate.query("""
                SELECT id, points_balance FROM member WHERE phone = ? FOR UPDATE
                """, (rs, rowNum) -> new MemberBalance(
                rs.getObject("id", UUID.class), rs.getLong("points_balance")), phone.trim())
                .stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("MEMBER_NOT_FOUND", "会员不存在: " + phone));
    }

    private record MemberBalance(UUID id, long points) {
    }
}
