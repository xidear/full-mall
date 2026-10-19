package com.fullmall.coupon.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 优惠券：模板管理与会员领券。
 *
 * <p>**本轮不含结算抵扣**——抵扣属于促销体系，需先确定"叠加还是互斥"的策略，不擅自接入订单金额。
 *
 * <p>防超发：领券时用**条件更新**（`issued_quantity < total_quantity`）原子占用库存，
 * 更新失败即视为已领完，不做"先查后写"的竞态处理。
 */
@Service
public class CouponService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private final JdbcTemplate jdbcTemplate;

    public CouponService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record TemplateView(UUID id, String code, String title, String type, long value,
                               long minOrderAmount, int totalQuantity, int issuedQuantity,
                               int perMemberLimit, int validityDays, String status) {
    }

    public record Coupon(UUID id, UUID templateId, String code, String status,
                         Instant expiresAt, String title, String type, long value) {
    }

    @Transactional
    public UUID createTemplate(String code, String title, String type, long value, long minOrderAmount,
                              int totalQuantity, int perMemberLimit, int validityDays,
                              Instant startsAt, Instant endsAt) {
        validateTemplate(code, type, value, totalQuantity, perMemberLimit, validityDays, startsAt, endsAt);

        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO coupon_template (id, code, title, type, value, min_order_amount,
                                             total_quantity, per_member_limit, validity_days,
                                             starts_at, ends_at, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT')
                """, id, code, title, type, value, minOrderAmount, totalQuantity, perMemberLimit,
                validityDays, java.sql.Timestamp.from(startsAt), java.sql.Timestamp.from(endsAt));
        return id;
    }

    @Transactional
    public void activate(UUID templateId) {
        int updated = jdbcTemplate.update(
                "UPDATE coupon_template SET status = 'ACTIVE' WHERE id = ? AND status = 'DRAFT'",
                templateId);
        if (updated != 1) {
            throw ApiException.conflict("COUPON_TEMPLATE_NOT_ACTIVATABLE",
                    "只有草稿状态的券模板可以启用: " + templateId);
        }
    }

    /** 会员领券：校验模板生效、时间窗、库存、每人限领。 */
    @Transactional
    public UUID claim(UUID templateId, UUID memberId) {
        List<TemplateRow> rows = jdbcTemplate.query("""
                SELECT id, type, value, total_quantity, issued_quantity, per_member_limit, validity_days,
                       starts_at, ends_at, status
                  FROM coupon_template WHERE id = ?
                """, (rs, rowNum) -> new TemplateRow(
                rs.getObject("id", UUID.class), rs.getString("type"), rs.getLong("value"),
                rs.getInt("total_quantity"), rs.getInt("issued_quantity"), rs.getInt("per_member_limit"),
                rs.getInt("validity_days"), rs.getTimestamp("starts_at").toInstant(),
                rs.getTimestamp("ends_at").toInstant(), rs.getString("status")), templateId);

        if (rows.isEmpty()) {
            throw ApiException.notFound("COUPON_TEMPLATE_NOT_FOUND", "券模板不存在: " + templateId);
        }
        TemplateRow template = rows.get(0);
        Instant now = Instant.now();
        if (!"ACTIVE".equals(template.status())) {
            throw ApiException.conflict("COUPON_TEMPLATE_INACTIVE", "券未开始发放: " + template.status());
        }
        if (!template.startsAt().isBefore(now) || !template.endsAt().isAfter(now)) {
            throw ApiException.conflict("COUPON_OUT_OF_WINDOW", "不在券的有效期内");
        }

        Integer claimed = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM member_coupon WHERE template_id = ? AND member_id = ?
                """, Integer.class, templateId, memberId);
        if (claimed != null && claimed >= template.perMemberLimit()) {
            throw ApiException.conflict("COUPON_LIMIT_REACHED",
                    "每人限领 " + template.perMemberLimit() + " 张，已领 " + claimed + " 张");
        }

        int reserved = jdbcTemplate.update("""
                UPDATE coupon_template SET issued_quantity = issued_quantity + 1
                 WHERE id = ? AND issued_quantity < total_quantity
                """, templateId);
        if (reserved != 1) {
            throw ApiException.conflict("COUPON_SOLD_OUT", "券已领完");
        }

        // 券的过期时间 = min(领取日 + 有效天数, 模板结束时间)
        Instant expiresAt = now.plusSeconds(template.validityDays() * 86400L);
        if (expiresAt.isAfter(template.endsAt())) {
            expiresAt = template.endsAt();
        }

        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO member_coupon (id, template_id, member_id, code, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """, id, templateId, memberId, randomCode(), java.sql.Timestamp.from(expiresAt));
        return id;
    }

    public List<Coupon> listMine(UUID memberId, String status, int limit) {
        if (limit <= 0 || limit > 100) {
            throw ApiException.badRequest("INVALID_LIMIT", "查询条数必须在 1..100，实际=" + limit);
        }
        String filter = status == null || status.isBlank() ? null : status.trim().toUpperCase(java.util.Locale.ROOT);
        // 返回时按过期时间实时判定：UNUSED 但已过期 → EXPIRED（不依赖定时任务才能正确显示）
        return jdbcTemplate.query("""
                SELECT c.id, c.template_id, c.code,
                       CASE WHEN c.status = 'UNUSED' AND c.expires_at <= now() THEN 'EXPIRED' ELSE c.status END AS status,
                       c.expires_at, t.title, t.type, t.value
                  FROM member_coupon c JOIN coupon_template t ON t.id = c.template_id
                 WHERE c.member_id = ?
                   AND (?::text IS NULL OR
                        (CASE WHEN c.status = 'UNUSED' AND c.expires_at <= now() THEN 'EXPIRED' ELSE c.status END) = ?::text)
                 ORDER BY c.issued_at DESC LIMIT ?
                """, (rs, rowNum) -> new Coupon(
                rs.getObject("id", UUID.class), rs.getObject("template_id", UUID.class),
                rs.getString("code"), rs.getString("status"),
                rs.getTimestamp("expires_at").toInstant(), rs.getString("title"),
                rs.getString("type"), rs.getLong("value")), memberId, filter, filter, limit);
    }

    private void validateTemplate(String code, String type, long value, int totalQuantity,
                                  int perMemberLimit, int validityDays, Instant startsAt, Instant endsAt) {
        if (code == null || !code.matches("^[A-Z0-9][A-Z0-9_-]{1,63}$")) {
            throw ApiException.badRequest("INVALID_COUPON_CODE", "模板编码只能是大写字母/数字/-/_");
        }
        if (!List.of("AMOUNT", "PERCENT").contains(type)) {
            throw ApiException.badRequest("INVALID_COUPON_TYPE", "券类型只能是 AMOUNT / PERCENT");
        }
        if (value <= 0) {
            throw ApiException.badRequest("INVALID_COUPON_VALUE", "券面额必须大于 0");
        }
        if ("PERCENT".equals(type) && value >= 100) {
            throw ApiException.badRequest("INVALID_COUPON_VALUE", "折扣券百分比必须在 1..99");
        }
        if (totalQuantity <= 0 || perMemberLimit <= 0 || validityDays <= 0) {
            throw ApiException.badRequest("INVALID_COUPON_QUANTITY", "总量/限领/有效天数必须大于 0");
        }
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw ApiException.badRequest("INVALID_COUPON_WINDOW", "结束时间必须晚于开始时间");
        }
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private record TemplateRow(UUID id, String type, long value, int totalQuantity, int issuedQuantity,
                               int perMemberLimit, int validityDays, Instant startsAt, Instant endsAt,
                               String status) {
    }
}
