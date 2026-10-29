package com.fullmall.member.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.setting.app.SystemSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 积分过期。
 *
 * <p>规则来自后台配置（**默认永不过期**）：
 * <ul>
 *   <li>`member.points.expiry-mode`：`NEVER`（默认）/ `ROLLING_DAYS` / `FIFO`；</li>
 *   <li>`member.points.expiry-days`：有效期天数（NEVER 时忽略，其余必须 > 0）。</li>
 * </ul>
 *
 * <p>过期按**逐笔余额**扣（`remaining_points`），只对 `EARN` 行生效；
 * 每笔写一条 `EXPIRE` 流水，余额不会为负。`FIFO` 与 `ROLLING_DAYS` 目前计算口径相同
 * （都是"获得后 N 天"），保留两种取值是为了将来支持"统一到期日"。
 */
@Service
public class PointsExpiryService {

    private static final Logger log = LoggerFactory.getLogger(PointsExpiryService.class);

    public static final String MODE_KEY = "member.points.expiry-mode";
    public static final String DAYS_KEY = "member.points.expiry-days";

    public enum Mode { NEVER, ROLLING_DAYS, FIFO }

    public record Settings(Mode mode, int days) {
    }

    public record Expired(UUID memberId, long points) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final SystemSettingService settings;

    public PointsExpiryService(JdbcTemplate jdbcTemplate, SystemSettingService settings) {
        this.jdbcTemplate = jdbcTemplate;
        this.settings = settings;
    }

    /** 读取并校验配置；非法值直接报错，不做静默回落（会误删顾客积分）。 */
    public Settings settings() {
        String rawMode = settings.rawValue(MODE_KEY);
        String normalized = rawMode == null || rawMode.isBlank()
                ? "NEVER" : rawMode.trim().toUpperCase(Locale.ROOT);
        Mode mode;
        try {
            mode = Mode.valueOf(normalized);
        } catch (IllegalArgumentException error) {
            throw ApiException.conflict("POINTS_EXPIRY_MODE_INVALID",
                    MODE_KEY + " 只能是 NEVER / ROLLING_DAYS / FIFO，实际=" + rawMode);
        }

        String rawDays = settings.rawValue(DAYS_KEY);
        int days = 0;
        if (rawDays != null && !rawDays.isBlank()) {
            try {
                days = Integer.parseInt(rawDays.trim());
            } catch (NumberFormatException error) {
                throw ApiException.conflict("POINTS_EXPIRY_DAYS_INVALID",
                        DAYS_KEY + " 必须是整数，实际=" + rawDays);
            }
        }
        if (mode != Mode.NEVER && days <= 0) {
            throw ApiException.conflict("POINTS_EXPIRY_DAYS_INVALID",
                    "启用过期时 " + DAYS_KEY + " 必须大于 0，实际=" + days);
        }
        if (days > 3650) {
            throw ApiException.conflict("POINTS_EXPIRY_DAYS_INVALID",
                    DAYS_KEY + " 不能超过 3650 天，实际=" + days);
        }
        return new Settings(mode, days);
    }

    /** 执行过期；NEVER 时直接返回空（不扫描、不改动）。 */
    @Transactional
    public List<Expired> expireNow(Instant now) {
        Settings config = settings();
        if (config.mode() == Mode.NEVER) {
            return List.of();
        }
        Instant cutoff = now.minus(config.days(), ChronoUnit.DAYS);

        // 先收集候选，再按会员逐个加锁：所有积分扣减统一按「会员行 → 积分账本行」顺序，
        // 避免与 redeem / reward 的加锁顺序相反造成死锁。
        List<Row> candidates = jdbcTemplate.query("""
                SELECT id, member_id, remaining_points FROM points_ledger
                 WHERE entry_type = 'EARN' AND remaining_points > 0 AND created_at <= ?
                 ORDER BY created_at, id
                """, (rs, rowNum) -> new Row(rs.getObject("id", UUID.class),
                rs.getObject("member_id", UUID.class), rs.getLong("remaining_points")),
                java.sql.Timestamp.from(cutoff));
        Map<UUID, List<Row>> byMember = new LinkedHashMap<>();
        for (Row row : candidates) {
            byMember.computeIfAbsent(row.memberId(), ignored -> new java.util.ArrayList<>()).add(row);
        }

        List<Expired> expired = new java.util.ArrayList<>();
        for (Map.Entry<UUID, List<Row>> entry : byMember.entrySet()) {
            UUID memberId = entry.getKey();
            lockMember(memberId);
            // 加锁后重新读取，避免候选阶段与并发消费/过期重叠。
            List<Row> rows = jdbcTemplate.query("""
                    SELECT id, member_id, remaining_points FROM points_ledger
                     WHERE member_id = ? AND entry_type = 'EARN' AND remaining_points > 0
                       AND created_at <= ?
                     ORDER BY created_at, id
                     FOR UPDATE
                    """, (rs, rowNum) -> new Row(rs.getObject("id", UUID.class),
                    rs.getObject("member_id", UUID.class), rs.getLong("remaining_points")),
                    memberId, java.sql.Timestamp.from(cutoff));
            for (Row row : rows) {
                long balance = balanceOf(memberId);
                long amount = Math.min(row.remaining(), Math.max(balance, 0));
                if (amount <= 0) {
                    continue;
                }
                int inserted = jdbcTemplate.update("""
                        INSERT INTO points_ledger (id, member_id, sale_id, entry_type, points,
                                                   balance_after, reason, business_key)
                        VALUES (?, ?, NULL, 'EXPIRE', ?, ?, '积分过期', ?)
                        ON CONFLICT (business_key) DO NOTHING
                        """, UUID.randomUUID(), memberId, -amount, balance - amount,
                        "expire:" + row.id());
                if (inserted != 1) {
                    throw new IllegalStateException("积分过期流水冲突：ledger=" + row.id());
                }
                int ledgerUpdated = jdbcTemplate.update(
                        "UPDATE points_ledger SET remaining_points = remaining_points - ?"
                                + " WHERE id = ? AND remaining_points >= ?",
                        amount, row.id(), amount);
                if (ledgerUpdated != 1) {
                    throw new IllegalStateException("积分账本过期扣减冲突：ledger=" + row.id());
                }
                int memberUpdated = jdbcTemplate.update(
                        "UPDATE member SET points_balance = points_balance - ?"
                                + " WHERE id = ? AND points_balance >= ?",
                        amount, memberId, amount);
                if (memberUpdated != 1) {
                    throw new IllegalStateException("会员积分过期扣减冲突：member=" + memberId);
                }
                expired.add(new Expired(memberId, amount));
            }
        }
        if (!expired.isEmpty()) {
            log.info("积分过期完成：{} 笔，共 {} 分", expired.size(),
                    expired.stream().mapToLong(Expired::points).sum());
        }
        return expired;
    }

    private void lockMember(UUID memberId) {
        List<UUID> locked = jdbcTemplate.queryForList(
                "SELECT id FROM member WHERE id = ? FOR UPDATE", UUID.class, memberId);
        if (locked.isEmpty()) {
            throw new IllegalStateException("会员不存在：" + memberId);
        }
    }

    private long balanceOf(UUID memberId) {
        Long value = jdbcTemplate.queryForObject(
                "SELECT points_balance FROM member WHERE id = ?", Long.class, memberId);
        return value == null ? 0 : value;
    }

    private record Row(UUID id, UUID memberId, long remaining) {
    }
}
