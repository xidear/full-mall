package com.fullmall.distribution.app;

import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 提现：申请 → 审核 → 打款。
 *
 * <p>关键约束：
 * <ul>
 *   <li>可提现金额 = AVAILABLE 佣金合计 − 处理中（PENDING/APPROVED）提现合计；</li>
 *   <li>打款时按 FIFO 把对应佣金置为 `WITHDRAWN` 并写流水，**账实必须一致**；
 *       若可用佣金不足以覆盖，直接失败（不产生负数或悬空余额）；</li>
 *   <li>拒绝必须写原因；打款必须有支付凭证号。</li>
 * </ul>
 */
@Service
public class WithdrawalService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditEventService auditEvents;

    public WithdrawalService(JdbcTemplate jdbcTemplate, AuditEventService auditEvents) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditEvents = auditEvents;
    }

    public record Withdrawal(UUID id, UUID promoterId, long amount, String currency, String status) {
    }

    @Transactional
    public UUID request(UUID promoterId, long amount, String currency) {
        if (promoterId == null) {
            throw ApiException.badRequest("PROMOTER_REQUIRED", "缺少提现人");
        }
        if (amount <= 0) {
            throw ApiException.badRequest("INVALID_WITHDRAWAL_AMOUNT", "提现金额必须大于 0");
        }
        long available = availableForWithdrawal(promoterId);
        if (amount > available) {
            throw ApiException.conflict("INSUFFICIENT_WITHDRAWABLE_BALANCE",
                    "可提现余额不足：可提现 " + available + "，申请 " + amount);
        }
        UUID id = UUID.randomUUID();
        int inserted = jdbcTemplate.update("""
                INSERT INTO withdrawal (id, promoter_id, amount_amount, currency, status)
                VALUES (?, ?, ?, ?, 'PENDING')
                """, id, promoterId, amount, currency);
        if (inserted != 1) {
            throw new IllegalStateException("提现申请写入失败");
        }
        audit("WITHDRAWAL_REQUESTED", id, promoterId.toString(), "amount=" + amount);
        return id;
    }

    @Transactional
    public void approve(UUID withdrawalId, String actor) {
        requireActor(actor);
        int updated = jdbcTemplate.update("""
                UPDATE withdrawal SET status = 'APPROVED', reviewed_by = ?, reviewed_at = now()
                 WHERE id = ? AND status = 'PENDING'
                """, actor, withdrawalId);
        if (updated != 1) {
            throw ApiException.conflict("WITHDRAWAL_NOT_PENDING",
                    "提现单不在待审核状态: " + withdrawalId);
        }
        audit("WITHDRAWAL_APPROVED", withdrawalId, actor, null);
    }

    @Transactional
    public void reject(UUID withdrawalId, String reason, String actor) {
        requireActor(actor);
        if (reason == null || reason.isBlank()) {
            throw ApiException.badRequest("REJECT_REASON_REQUIRED", "拒绝提现必须填写原因");
        }
        int updated = jdbcTemplate.update("""
                UPDATE withdrawal SET status = 'REJECTED', reject_reason = ?, reviewed_by = ?,
                       reviewed_at = now()
                 WHERE id = ? AND status IN ('PENDING', 'APPROVED')
                """, reason.trim(), actor, withdrawalId);
        if (updated != 1) {
            throw ApiException.conflict("WITHDRAWAL_NOT_REJECTABLE",
                    "提现单当前状态不可拒绝: " + withdrawalId);
        }
        audit("WITHDRAWAL_REJECTED", withdrawalId, actor, reason.trim());
    }

    /** 打款：标记 PAID，并把等额 AVAILABLE 佣金按 FIFO 置为 WITHDRAWN。 */
    @Transactional
    public void markPaid(UUID withdrawalId, String paymentRef, String actor) {
        requireActor(actor);
        if (paymentRef == null || paymentRef.isBlank()) {
            throw ApiException.badRequest("PAYMENT_REF_REQUIRED", "打款必须记录支付凭证号");
        }
        Withdrawal withdrawal = find(withdrawalId);
        if (!"APPROVED".equals(withdrawal.status())) {
            throw ApiException.conflict("WITHDRAWAL_NOT_APPROVED",
                    "只有已审核的提现单可以打款，当前=" + withdrawal.status());
        }

        long remaining = withdrawal.amount();
        List<UUID> commissions = jdbcTemplate.query("""
                SELECT id FROM distribution_commission
                 WHERE promoter_staff_id = ? AND status = 'AVAILABLE'
                 ORDER BY available_at, created_at
                 FOR UPDATE
                """, (rs, rowNum) -> rs.getObject("id", UUID.class), withdrawal.promoterId());

        for (UUID commissionId : commissions) {
            if (remaining <= 0) {
                break;
            }
            Long commissionAmount = jdbcTemplate.queryForObject(
                    "SELECT commission_amount FROM distribution_commission WHERE id = ?",
                    Long.class, commissionId);
            if (commissionAmount == null) {
                throw new IllegalStateException("佣金记录丢失: " + commissionId);
            }
            if (commissionAmount > remaining) {
                // 部分消耗不支持（佣金不可拆分），跳过这条，继续找更小的
                continue;
            }
            int updated = jdbcTemplate.update("""
                    UPDATE distribution_commission SET status = 'WITHDRAWN'
                     WHERE id = ? AND status = 'AVAILABLE'
                    """, commissionId);
            if (updated == 1) {
                jdbcTemplate.update("""
                        INSERT INTO commission_ledger (id, commission_id, from_status, to_status,
                                                       amount_amount, currency, reason)
                        VALUES (?, ?, 'AVAILABLE', 'WITHDRAWN', ?, ?, ?)
                        """, UUID.randomUUID(), commissionId, commissionAmount,
                        withdrawal.currency(), "withdrawal " + withdrawalId);
                remaining -= commissionAmount;
            }
        }

        if (remaining > 0) {
            throw ApiException.conflict("WITHDRAWAL_COMMISSION_MISMATCH",
                    "可用佣金不足以覆盖提现金额，剩余未覆盖 " + remaining + "，需人工核对");
        }

        int updated = jdbcTemplate.update("""
                UPDATE withdrawal SET status = 'PAID', paid_at = now(), payment_ref = ?
                 WHERE id = ? AND status = 'APPROVED'
                """, paymentRef.trim(), withdrawalId);
        if (updated != 1) {
            throw new IllegalStateException("提现单打款状态写入失败: " + withdrawalId);
        }
        audit("WITHDRAWAL_PAID", withdrawalId, actor, "ref=" + paymentRef.trim());
    }

    /** 可提现 = AVAILABLE 合计 − 处理中提现合计。 */
    public long availableForWithdrawal(UUID promoterId) {
        Long available = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(commission_amount), 0) FROM distribution_commission
                 WHERE promoter_staff_id = ? AND status = 'AVAILABLE'
                """, Long.class, promoterId);
        Long pending = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_amount), 0) FROM withdrawal
                 WHERE promoter_id = ? AND status IN ('PENDING', 'APPROVED')
                """, Long.class, promoterId);
        if (available == null || pending == null) {
            throw new IllegalStateException("提现余额查询返回 null: promoter=" + promoterId);
        }
        return available - pending;
    }

    public List<Withdrawal> list(String status, int limit) {
        if (limit <= 0 || limit > 200) {
            throw ApiException.badRequest("INVALID_LIMIT", "查询条数必须在 1..200，实际=" + limit);
        }
        String filter = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        return jdbcTemplate.query("""
                SELECT id, promoter_id, amount_amount, currency, status FROM withdrawal
                 WHERE (?::text IS NULL OR status = ?::text)
                 ORDER BY requested_at DESC LIMIT ?
                """, (rs, rowNum) -> new Withdrawal(
                rs.getObject("id", UUID.class), rs.getObject("promoter_id", UUID.class),
                rs.getLong("amount_amount"), rs.getString("currency"), rs.getString("status")),
                filter, filter, limit);
    }

    public Withdrawal find(UUID id) {
        return jdbcTemplate.query("""
                SELECT id, promoter_id, amount_amount, currency, status FROM withdrawal WHERE id = ?
                """, (rs, rowNum) -> new Withdrawal(
                rs.getObject("id", UUID.class), rs.getObject("promoter_id", UUID.class),
                rs.getLong("amount_amount"), rs.getString("currency"), rs.getString("status")), id)
                .stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("WITHDRAWAL_NOT_FOUND", "提现单不存在: " + id));
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw ApiException.badRequest("ACTOR_REQUIRED", "审核/打款必须记录操作人");
        }
    }

    private void audit(String type, UUID withdrawalId, String actor, String detail) {
        auditEvents.record(new AuditEventService.AuditEvent(
                type, actor, "withdrawal", withdrawalId.toString(), detail, null));
    }
}
