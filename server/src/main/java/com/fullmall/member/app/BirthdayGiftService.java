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
    /** 业务时区：中国大陆。 */
    public static final ZoneOffset BUSINESS_OFFSET = ZoneOffset.ofHours(8);

    private final JdbcTemplate jdbcTemplate;

    public BirthdayGiftService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Granted(UUID memberId, long points) {
    }

    /** 发放"今天生日"的会员礼积分，返回本次实际发放列表。 */
    @Transactional
    public List<Granted> grantForDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("发放日期不能为空");
        }
        int year = date.getYear();
        List<Row> members = jdbcTemplate.query("""
                SELECT m.id, m.points_balance, l.birthday_points
                  FROM member m JOIN member_level l ON l.code = m.tier
                 WHERE m.birthday IS NOT NULL
                   AND EXTRACT(MONTH FROM m.birthday) = ?
                   AND EXTRACT(DAY FROM m.birthday) = ?
                   AND l.birthday_points > 0
                   AND (m.birthday_gift_year IS NULL OR m.birthday_gift_year <> ?)
                """, (rs, rowNum) -> new Row(
                rs.getObject("id", UUID.class), rs.getLong("points_balance"),
                rs.getLong("birthday_points")),
                date.getMonthValue(), date.getDayOfMonth(), year);

        return members.stream()
                .map(member -> grant(member, year))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private Granted grant(Row member, int year) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO points_ledger (id, member_id, sale_id, entry_type, points,
                                           balance_after, reason, business_key)
                VALUES (?, ?, NULL, 'EARN', ?, ?, ?, ?)
                ON CONFLICT (business_key) DO NOTHING
                """, UUID.randomUUID(), member.id(), member.birthdayPoints(),
                member.points() + member.birthdayPoints(), "生日礼积分",
                "birthday:" + member.id() + ":" + year);
        if (inserted != 1) {
            return null;   // 已发过（并发/重复执行），不重复加余额
        }
        jdbcTemplate.update("UPDATE member SET points_balance = points_balance + ?,"
                + " birthday_gift_year = ? WHERE id = ?",
                member.birthdayPoints(), year, member.id());
        log.info("生日礼积分发放：member={} year={} points={}", member.id(), year, member.birthdayPoints());
        return new Granted(member.id(), member.birthdayPoints());
    }

    private record Row(UUID id, long points, long birthdayPoints) {
    }
}
