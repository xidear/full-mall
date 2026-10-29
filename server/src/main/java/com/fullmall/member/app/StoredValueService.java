package com.fullmall.member.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 储值充值/扣款。
 *
 * <p>规则：
 * <ul>
 *   <li>**幂等**：`business_key` 唯一，重复提交不重复加钱（充值是最容易被重复点击的操作）；</li>
 *   <li>锁会员行（`FOR UPDATE`）后读余额再写，避免并发充值丢失更新；</li>
 *   <li>金额必须 > 0；余额不允许为负；每次变动写一条流水。</li>
 * </ul>
 */
@Service
public class StoredValueService {

    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final String disabledReason;

    public StoredValueService(
            JdbcTemplate jdbc,
            @org.springframework.beans.factory.annotation.Value("${app.stored-value.enabled:false}")
            boolean enabled,
            @org.springframework.beans.factory.annotation.Value(
                    "${app.stored-value.disabled-reason:储值（预付卡）功能需相应资质，默认关闭}")
            String disabledReason) {
        this.jdbc = jdbc;
        this.enabled = enabled;
        this.disabledReason = disabledReason;
    }

    public record Result(long balance, boolean duplicated) {
    }

    @Transactional
    public Result topUp(UUID memberId, long amount, String businessKey, String note) {
        if (!enabled) {
            // 预付卡属特许经营：未取得资质不得开放充值，默认关闭
            throw ApiException.conflict("STORED_VALUE_DISABLED", disabledReason);
        }
        if (amount <= 0) {
            throw ApiException.badRequest("STORED_VALUE_AMOUNT_INVALID", "充值金额必须大于 0");
        }
        if (businessKey == null || businessKey.isBlank()) {
            throw ApiException.badRequest("STORED_VALUE_KEY_REQUIRED", "充值必须提供幂等键");
        }
        Long balance = jdbc.query("SELECT stored_value_balance FROM member WHERE id = ? FOR UPDATE",
                rs -> rs.next() ? rs.getLong(1) : null, memberId);
        if (balance == null) {
            throw ApiException.notFound("MEMBER_NOT_FOUND", "会员不存在：" + memberId);
        }
        long next = balance + amount;
        int inserted = jdbc.update("""
                INSERT INTO stored_value_ledger (id, member_id, entry_type, amount, balance_after,
                                                 business_key, note)
                VALUES (?, ?, 'TOPUP', ?, ?, ?, ?)
                ON CONFLICT (business_key) DO NOTHING
                """, UUID.randomUUID(), memberId, amount, next, businessKey.trim(), note);
        if (inserted == 0) {
            // 幂等命中：余额不再变动，返回当前余额
            Long current = jdbc.queryForObject(
                    "SELECT stored_value_balance FROM member WHERE id = ?", Long.class, memberId);
            if (current == null) {
                throw new IllegalStateException("会员储值余额查询返回 null: " + memberId);
            }
            return new Result(current, true);
        }
        int updated = jdbc.update(
                "UPDATE member SET stored_value_balance = ? WHERE id = ?", next, memberId);
        if (updated != 1) {
            throw new IllegalStateException("会员储值余额更新失败: " + memberId);
        }
        return new Result(next, false);
    }
}
