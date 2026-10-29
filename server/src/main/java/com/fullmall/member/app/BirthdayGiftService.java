package com.fullmall.member.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * 生日礼积分：会员生日当天按其等级发放 `member_level.birthday_points`，**每年一次**。
 *
 * <p>幂等由 `birthday_gift_year` 保证（当年已发则跳过），并用 `points_ledger.business_key`
 * 作为第二道防线（`birthday:<member>:<year>`），并发下也不会重复入账。
 *
 * <p>时区：按固定业务时区（东八区）判断"今天"，避免服务器 UTC 导致生日错一天。
 */
@Service
public class BirthdayGiftService {

    private static final Logger log = LoggerFactory.getLogger(BirthdayGiftService.class);
    /** 系统默认时区配置键（未设置时取服务器时区）。 */
    public static final String SYSTEM_ZONE = "app.timezone";

    private final JdbcTemplate jdbcTemplate;
    private final com.fullmall.setting.app.SystemSettingService systemSettings;

    public BirthdayGiftService(JdbcTemplate jdbcTemplate,
                               com.fullmall.setting.app.SystemSettingService systemSettings) {
        this.jdbcTemplate = jdbcTemplate;
        this.systemSettings = systemSettings;
    }

    public record Granted(UUID memberId, long points) {
    }

    /**
     * 按**会员所在时区**发放生日礼积分（不跨时区顺延、不补发）。
     *
     * <p>为什么不能固定 UTC+8：会员在 UTC+14 或 UTC-8 时，"今天是不是生日"完全不同；
     * 用服务器时区会导致**提前或延后一天发放**。
     *
     * <p>为什么不顺延补发：否则可通过改时区/跨日反复触发，形成重复领取漏洞；
     * 任务当天没跑就视为错过，由运维补跑（补跑仍按会员本地日期判定）。
     */
    @Transactional
    public List<Granted> grantAt(java.time.Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("发放时间不能为空");
        }
        List<Candidate> candidates = jdbcTemplate.query("""
                SELECT m.id, m.points_balance, l.birthday_points, m.birthday, m.timezone
                  FROM member m JOIN member_level l ON l.code = m.tier
                 WHERE m.birthday IS NOT NULL AND l.birthday_points > 0
                 ORDER BY m.id
                 LIMIT 5000
                """, (rs, rowNum) -> new Candidate(
                rs.getObject("id", UUID.class), rs.getLong("points_balance"),
                rs.getLong("birthday_points"),
                rs.getDate("birthday").toLocalDate(), rs.getString("timezone")));

        List<Granted> granted = new java.util.ArrayList<>();
        for (Candidate candidate : candidates) {
            java.time.ZoneId zone = zoneOf(candidate.timezone());
            LocalDate localToday = now.atZone(zone).toLocalDate();
            boolean birthdayToday = candidate.birthday().getMonthValue() == localToday.getMonthValue()
                    && candidate.birthday().getDayOfMonth() == localToday.getDayOfMonth();
            if (!birthdayToday) {
                continue;
            }
            Granted result = grant(candidate, localToday.getYear());
            if (result != null) {
                granted.add(result);
            }
        }
        return granted;
    }

    /**
     * 会员时区：会员未设置时**跟随系统默认**（系统默认 = 服务器时区，可由 `app.timezone` 覆盖）。
     *
     * <p>时区字符串非法直接抛错，不静默回落到默认值（会错一天）。
     */
    private java.time.ZoneId zoneOf(String memberZone) {
        String raw = memberZone == null || memberZone.isBlank() ? systemZone() : memberZone.trim();
        try {
            return java.time.ZoneId.of(raw);
        } catch (java.time.DateTimeException error) {
            throw new IllegalStateException("时区配置无效：" + raw, error);
        }
    }

    private String systemZone() {
        String configured = systemSettings.rawValue(SYSTEM_ZONE);
        return configured == null || configured.isBlank()
                ? java.time.ZoneId.systemDefault().getId() : configured.trim();
    }

    /** 兼容旧调用：按"现在"发放。 */
    public List<Granted> grantToday() {
        return grantAt(java.time.Instant.now());
    }

    private record Candidate(UUID id, long points, long birthdayPoints,
                             LocalDate birthday, String timezone) {
    }

    private Granted grant(Candidate member, int year) {
        // 幂等：当年已发则跳过（`birthday_gift_year`）
        Integer already = jdbcTemplate.queryForObject(
                "SELECT birthday_gift_year FROM member WHERE id = ? FOR UPDATE", Integer.class, member.id());
        if (already != null && already == year) {
            return null;
        }
        int inserted = jdbcTemplate.update("""
                INSERT INTO points_ledger (id, member_id, sale_id, entry_type, points,
                                           balance_after, reason, business_key)
                VALUES (?, ?, NULL, 'EARN', ?, ?, ?, ?)
                ON CONFLICT (business_key) DO NOTHING
                """, UUID.randomUUID(), member.id(), member.birthdayPoints(),
                member.points() + member.birthdayPoints(), "生日礼积分",
                "birthday:" + member.id() + ":" + year);
        if (inserted != 1) {
            return null;
        }
        int updated = jdbcTemplate.update("UPDATE member SET points_balance = points_balance + ?,"
                + " birthday_gift_year = ? WHERE id = ?"
                + " AND (birthday_gift_year IS NULL OR birthday_gift_year <> ?)",
                member.birthdayPoints(), year, member.id(), year);
        if (updated != 1) {
            throw new IllegalStateException("生日礼积分入账并发冲突：member=" + member.id());
        }
        log.info("生日礼积分发放：member={} year={} points={}", member.id(), year, member.birthdayPoints());
        return new Granted(member.id(), member.birthdayPoints());
    }
}
