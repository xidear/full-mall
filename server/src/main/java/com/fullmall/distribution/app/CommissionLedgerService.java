package com.fullmall.distribution.app;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 佣金生命周期（分销）。
 *
 * <p>状态机：`FROZEN → AVAILABLE → WITHDRAWN`，任意非终态可 `REVERSED`（退款回冲）。
 * 每次状态变更都写一条不可变流水 `commission_ledger`，**不允许只改状态不留痕**。
 *
 * <p>为什么不是"支付即得"：售后窗口内退款要能扣回，所以先冻结；
 * 冻结期满且无售后才转可提现。这是成熟商城分销的通用做法。
 */
@Service
public class CommissionLedgerService {

    private final JdbcTemplate jdbcTemplate;

    public CommissionLedgerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Commission(UUID id, UUID orderId, UUID promoterId, long amount, String currency,
                             String status, Instant frozenUntil) {
    }

    /** 冻结期默认 7 天（售后窗口）。 */
    public static final Duration DEFAULT_FREEZE = Duration.ofDays(7);

    /**
     * 处理到期的冻结佣金：FROZEN → AVAILABLE。
     *
     * @return 本次解冻条数
     */
    @Transactional
    public int releaseDue(Instant now) {
        List<Commission> due = jdbcTemplate.query("""
                SELECT id, order_id, promoter_staff_id, commission_amount, currency, status, frozen_until
                  FROM distribution_commission
                 WHERE status = 'FROZEN' AND frozen_until <= ?
                 ORDER BY frozen_until
                 LIMIT 500
                """, (rs, rowNum) -> map(rs), Timestamp.from(now));
        if (due.isEmpty()) {
            return 0;
        }
        int released = 0;
        for (Commission commission : due) {
            int updated = jdbcTemplate.update("""
                    UPDATE distribution_commission
                       SET status = 'AVAILABLE', available_at = ?
                     WHERE id = ? AND status = 'FROZEN'
                    """, Timestamp.from(now), commission.id());
            if (updated == 1) {
                writeLedger(commission, "FROZEN", "AVAILABLE", "freeze expired");
                released++;
            }
        }
        return released;
    }

    /**
     * 退款/取消回冲佣金：FROZEN 或 AVAILABLE → REVERSED。
     *
     * @return 是否发生回冲（已回冲或已提现的不会重复处理）
     */
    @Transactional
    public boolean reverse(UUID commissionId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("回冲必须填写原因: commission=" + commissionId);
        }
        Commission commission = findById(commissionId);
        if ("REVERSED".equals(commission.status())) {
            return false;
        }
        if ("WITHDRAWN".equals(commission.status())) {
            throw new IllegalStateException(
                    "佣金已提现，不能直接回冲，需走线下追回流程: " + commissionId);
        }
        int updated = jdbcTemplate.update("""
                UPDATE distribution_commission
                   SET status = 'REVERSED', reversed_at = now(), reverse_reason = ?
                 WHERE id = ? AND status IN ('FROZEN', 'AVAILABLE')
                """, reason.trim(), commissionId);
        if (updated != 1) {
            throw new IllegalStateException(
                    "佣金回冲失败（状态可能已变化）: commission=" + commissionId
                            + "，当前状态=" + commission.status());
        }
        writeLedger(commission, commission.status(), "REVERSED", reason.trim());
        return true;
    }

    /** 按订单回冲（一笔订单通常一条佣金）。 */
    @Transactional
    public int reverseByOrder(UUID orderId, String reason) {
        List<Commission> rows = jdbcTemplate.query("""
                SELECT id, order_id, promoter_staff_id, commission_amount, currency, status, frozen_until
                  FROM distribution_commission WHERE order_id = ?
                """, (rs, rowNum) -> map(rs), orderId);
        int reversed = 0;
        for (Commission commission : rows) {
            if (reverse(commission.id(), reason)) {
                reversed++;
            }
        }
        return reversed;
    }

    /** 可提现余额（只算 AVAILABLE）。 */
    public long availableBalance(UUID promoterId) {
        Long sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(commission_amount), 0) FROM distribution_commission
                 WHERE promoter_staff_id = ? AND status = 'AVAILABLE'
                """, Long.class, promoterId);
        if (sum == null) {
            throw new IllegalStateException("佣金余额查询返回 null: promoter=" + promoterId);
        }
        return sum;
    }

    public List<UUID> ledgerOf(UUID commissionId) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM commission_ledger WHERE commission_id = ? ORDER BY created_at",
                UUID.class, commissionId);
    }

    private Commission findById(UUID commissionId) {
        return jdbcTemplate.query("""
                SELECT id, order_id, promoter_staff_id, commission_amount, currency, status, frozen_until
                  FROM distribution_commission WHERE id = ?
                """, (rs, rowNum) -> map(rs), commissionId)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("佣金记录不存在: " + commissionId));
    }

    private void writeLedger(Commission commission, String from, String to, String reason) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO commission_ledger (id, commission_id, from_status, to_status,
                                               amount_amount, currency, reason)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), commission.id(), from, to,
                commission.amount(), commission.currency(), reason);
        if (inserted != 1) {
            throw new IllegalStateException("佣金流水写入失败: " + commission.id());
        }
    }

    private Commission map(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp frozen = rs.getTimestamp("frozen_until");
        return new Commission(
                rs.getObject("id", UUID.class),
                rs.getObject("order_id", UUID.class),
                rs.getObject("promoter_staff_id", UUID.class),
                rs.getLong("commission_amount"),
                rs.getString("currency"),
                rs.getString("status"),
                frozen == null ? null : frozen.toInstant());
    }
}