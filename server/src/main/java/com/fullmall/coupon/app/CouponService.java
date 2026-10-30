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
    private final com.fullmall.platform.audit.AuditEventService auditEvents;

    public CouponService(JdbcTemplate jdbcTemplate,
                         com.fullmall.platform.audit.AuditEventService auditEvents) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditEvents = auditEvents;
    }

    public record TemplateView(UUID id, String code, String title, String type, long value,
                               long minOrderAmount, int totalQuantity, int issuedQuantity,
                               int perMemberLimit, int validityDays, String status) {
    }

    public record Coupon(UUID id, UUID templateId, String code, String status,
                         Instant expiresAt, String title, String type, long value) {
    }

    /**
     * 建全场券。{@code @Transactional} 只在本重载上，类内自调用不会重开事务；带 scope 的重载以后
     * 若加入第二次写入，必须自己补注解，否则失败不会一起回滚。
     */
    @Transactional
    public UUID createTemplate(String code, String title, String type, long value, long minOrderAmount,
                              int totalQuantity, int perMemberLimit, int validityDays,
                              Instant startsAt, Instant endsAt) {
        return createTemplate(code, title, type, value, minOrderAmount, totalQuantity,
                perMemberLimit, validityDays, startsAt, endsAt, "ALL", java.util.List.of());
    }

    /** 带适用范围的建券（scope：ALL / SKU / CATEGORY / BRAND）。 */
    /**
     * 新券落 DRAFT，必须显式启用才能领；非 ALL 却没给 scopeIds 直接 400，否则会变成“看似限类目、实际全场可用”。
     */
    public UUID createTemplate(String code, String title, String type, long value, long minOrderAmount,
                              int totalQuantity, int perMemberLimit, int validityDays,
                              Instant startsAt, Instant endsAt, String scope,
                              java.util.List<UUID> scopeIds) {
        validateTemplate(code, type, value, totalQuantity, perMemberLimit, validityDays, startsAt, endsAt);

        String normalizedScope = scope == null || scope.isBlank() ? "ALL" : scope.trim().toUpperCase();
        if (!List.of("ALL", "SKU", "CATEGORY", "BRAND").contains(normalizedScope)) {
            throw ApiException.badRequest("INVALID_COUPON_SCOPE", "范围只能是 ALL/SKU/CATEGORY/BRAND");
        }
        if (!"ALL".equals(normalizedScope) && (scopeIds == null || scopeIds.isEmpty())) {
            throw ApiException.badRequest("COUPON_SCOPE_IDS_REQUIRED", "非全场券必须指定范围 ID");
        }
        String scopeJson = scopeIds == null ? "[]" : scopeIds.stream()
                .map(id -> "\"" + id + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));

        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO coupon_template (id, code, title, type, value, min_order_amount,
                                             total_quantity, per_member_limit, validity_days,
                                             starts_at, ends_at, status, scope, scope_ids)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, CAST(? AS jsonb))
                """, id, code, title, type, value, minOrderAmount, totalQuantity, perMemberLimit,
                validityDays, java.sql.Timestamp.from(startsAt), java.sql.Timestamp.from(endsAt),
                normalizedScope, scopeJson);
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

    /**
     * 修改券模板：**只有草稿可改**。
     *
     * <p>生效中的模板改了面额/门槛会让已领取的券与规则不一致——已发实例按领取时的快照执行，
     * 但运营口径会混乱；因此生效中只允许停用，不允许改参数。
     */
    @Transactional
    public void updateTemplate(UUID templateId, String title, long value, long minOrderAmount,
                               int totalQuantity, int perMemberLimit, int validityDays) {
        if (value <= 0) {
            throw ApiException.badRequest("COUPON_VALUE_INVALID", "面额必须大于 0");
        }
        if (totalQuantity <= 0 || perMemberLimit <= 0) {
            throw ApiException.badRequest("COUPON_QUANTITY_INVALID", "发放总量与每人限领必须大于 0");
        }
        if (validityDays <= 0 || validityDays > 365) {
            throw ApiException.badRequest("COUPON_VALIDITY_INVALID", "有效期天数必须在 1..365");
        }
        int updated = jdbcTemplate.update("""
                UPDATE coupon_template
                   SET title = ?, value = ?, min_order_amount = ?, total_quantity = ?,
                       per_member_limit = ?, validity_days = ?
                 WHERE id = ? AND status = 'DRAFT'
                """, title, value, minOrderAmount, totalQuantity, perMemberLimit, validityDays,
                templateId);
        if (updated != 1) {
            throw ApiException.conflict("COUPON_TEMPLATE_NOT_EDITABLE",
                    "只有草稿状态的券模板可以修改（已生效请先停用）: " + templateId);
        }
    }

    /** 停用模板：立即停止发放；已领取的券实例不受影响（按领取时的有效期继续可用）。 */
    @Transactional
    public void disable(UUID templateId) {
        int updated = jdbcTemplate.update("""
                UPDATE coupon_template SET status = 'DISABLED'
                 WHERE id = ? AND status IN ('DRAFT', 'ACTIVE')
                """, templateId);
        if (updated != 1) {
            throw ApiException.conflict("COUPON_TEMPLATE_NOT_DISABLABLE",
                    "该券模板当前状态不可停用: " + templateId);
        }
    }

    /**
     * 券模板效果统计。
     *
     * <p>口径（与 `member_coupon.status` 严格一致）：
     * <ul>
     *   <li>`claimRate` = 已领取 / 发放总量（投放被领走的比例）；</li>
     *   <li>`redemptionRate` = 已核销 / 已领取（领了有没有用，才是活动效果）；</li>
     *   <li>过期单独计数，不并入"未使用"，否则会高估余量。</li>
     * </ul>
     */
    public record CouponStats(UUID templateId, String code, int totalQuantity, int claimed,
                              int used, int expired, int unused, double claimRate,
                              double redemptionRate) {
    }

    public CouponStats stats(UUID templateId) {
        var row = jdbcTemplate.queryForMap("""
                SELECT t.id, t.code, t.total_quantity,
                       COUNT(c.id) AS claimed,
                       COUNT(c.id) FILTER (WHERE c.status = 'USED') AS used,
                       COUNT(c.id) FILTER (WHERE c.status = 'EXPIRED') AS expired,
                       COUNT(c.id) FILTER (WHERE c.status = 'UNUSED') AS unused
                  FROM coupon_template t
                  LEFT JOIN member_coupon c ON c.template_id = t.id
                 WHERE t.id = ?
                 GROUP BY t.id, t.code, t.total_quantity
                """, templateId);
        int total = ((Number) row.get("total_quantity")).intValue();
        int claimed = ((Number) row.get("claimed")).intValue();
        int used = ((Number) row.get("used")).intValue();
        int expired = ((Number) row.get("expired")).intValue();
        int unused = ((Number) row.get("unused")).intValue();
        double claimRate = total == 0 ? 0 : Math.min(1.0, (double) claimed / total);
        double redemptionRate = claimed == 0 ? 0 : (double) used / claimed;
        return new CouponStats(templateId, (String) row.get("code"), total, claimed, used,
                expired, unused, claimRate, redemptionRate);
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

        // 每人限领走"读-判-写"：member_coupon 在 (template_id, member_id) 上没有唯一约束，
        // 同一会员并发领券理论上可多领（超发上限仍由下面的 issued_quantity 条件更新挡住）。
        // 要堵死需要补一条唯一约束或换成带条件的 INSERT，改之前不要假设这里绝对精确。
        Integer claimed = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM member_coupon WHERE template_id = ? AND member_id = ?
                """, Integer.class, templateId, memberId);
        if (claimed != null && claimed >= template.perMemberLimit()) {
            throw ApiException.conflict("COUPON_LIMIT_REACHED",
                    "每人限领 " + template.perMemberLimit() + " 张，已领 " + claimed + " 张");
        }

        // 防超发的唯一权威：条件更新 issued_quantity < total_quantity，影响 0 行即"已领完"。
        // 不能用"先查 issued_quantity 再写"——那是无锁读改写，并发下必然多发。
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

    public record Eligibility(boolean usable, String reason, long discountAmount) {
    }

    /**
     * 券对某金额的可用性（**只读校验，不改订单金额、不改券状态**）。
     */
    /**
     * 券在给定商品明细下的**可用金额**（按模板范围过滤）。
     *
     * <p>`scope=ALL` 返回全部小计；`SKU/CATEGORY/BRAND` 只累加命中商品。
     * 与促销的范围口径一致，避免"促销按类目、券按全场"的错配。
     */
    public long eligibleAmount(UUID couponId, java.util.Map<UUID, Long> subtotalsBySku) {
        // 读取走关联表（真相）；scope_ids JSON 仅保留为写入投影
        List<ScopeRow> rows = jdbcTemplate.query("""
                SELECT t.scope,
                       COALESCE((SELECT jsonb_agg(s.scope_id::text ORDER BY s.scope_id)
                                   FROM coupon_template_scope s
                                  WHERE s.template_id = t.id), '[]'::jsonb)::text AS scope_ids
                  FROM coupon_template t
                  JOIN member_coupon c ON c.template_id = t.id WHERE c.id = ?
                """, (rs, rowNum) -> new ScopeRow(
                rs.getString("scope"), rs.getString("scope_ids")), couponId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("COUPON_NOT_FOUND", "券不存在: " + couponId);
        }
        if (subtotalsBySku == null || subtotalsBySku.isEmpty()) {
            return 0;
        }
        ScopeRow scope = rows.get(0);
        long total = subtotalsBySku.values().stream().mapToLong(Long::longValue).sum();
        if ("ALL".equals(scope.scope())) {
            return total;
        }

        java.util.Set<String> ids = new java.util.HashSet<>();
        try {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(scope.scopeIds())
                    .forEach(node -> ids.add(node.asText()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException("券范围解析失败: " + couponId, error);
        }

        java.util.Map<UUID, String> categoryBySku = new java.util.HashMap<>();
        java.util.Map<UUID, String> brandBySku = new java.util.HashMap<>();
        if ("CATEGORY".equals(scope.scope()) || "BRAND".equals(scope.scope())) {
            jdbcTemplate.query("""
                    SELECT s.id, p.category_id, p.brand_id
                      FROM sku s JOIN product p ON p.id = s.product_id
                     WHERE s.id = ANY(?)
                    """, rs -> {
                UUID skuId = rs.getObject("id", UUID.class);
                Object category = rs.getObject("category_id");
                Object brand = rs.getObject("brand_id");
                categoryBySku.put(skuId, category == null ? "" : category.toString());
                brandBySku.put(skuId, brand == null ? "" : brand.toString());
            }, (Object) subtotalsBySku.keySet().toArray(new UUID[0]));
        }

        long eligible = 0;
        for (var entry : subtotalsBySku.entrySet()) {
            String skuId = entry.getKey().toString();
            boolean hit = switch (scope.scope()) {
                case "SKU" -> ids.contains(skuId);
                case "CATEGORY" -> ids.contains(categoryBySku.getOrDefault(entry.getKey(), ""));
                case "BRAND" -> ids.contains(brandBySku.getOrDefault(entry.getKey(), ""));
                default -> false;
            };
            if (hit) {
                eligible += entry.getValue();
            }
        }
        return eligible;
    }

    private record ScopeRow(String scope, String scopeIds) {
    }

    /**
     * 券对某订单的可用性（只读）。金额单位为分：AMOUNT 券最多抵到 0，PERCENT 券用 BigDecimal HALF_UP
     * 取整到分（先 double 再转 long 会在 0.5 分上错 1 分）。不可用返回 usable=false + 原因，
     * 但券不存在/不属于该会员抛 404 —— 借号试券不能当成"不可用"。
     */
    public Eligibility checkUsable(UUID couponId, UUID memberId, long orderAmount) {
        if (orderAmount < 0) {
            throw ApiException.badRequest("INVALID_ORDER_AMOUNT", "订单金额不能为负");
        }
        List<EligibilityRow> rows = jdbcTemplate.query("""
                SELECT c.status, c.expires_at, t.type, t.value, t.min_order_amount
                  FROM member_coupon c JOIN coupon_template t ON t.id = c.template_id
                 WHERE c.id = ? AND c.member_id = ?
                """, (rs, rowNum) -> new EligibilityRow(
                rs.getString("status"), rs.getTimestamp("expires_at").toInstant(),
                rs.getString("type"), rs.getLong("value"), rs.getLong("min_order_amount")),
                couponId, memberId);

        if (rows.isEmpty()) {
            throw ApiException.notFound("COUPON_NOT_FOUND", "券不存在或不属于当前会员: " + couponId);
        }
        EligibilityRow coupon = rows.get(0);
        if (!"UNUSED".equals(coupon.status())) {
            return new Eligibility(false, "券已使用或不可用: " + coupon.status(), 0);
        }
        if (!coupon.expiresAt().isAfter(Instant.now())) {
            return new Eligibility(false, "券已过期", 0);
        }
        if (orderAmount < coupon.minOrderAmount()) {
            return new Eligibility(false, "未达到使用门槛，需满 " + coupon.minOrderAmount() + " 分", 0);
        }

        long discount = "AMOUNT".equals(coupon.type())
                ? Math.min(coupon.value(), orderAmount)
                : java.math.BigDecimal.valueOf(orderAmount)
                    .multiply(java.math.BigDecimal.valueOf(100 - coupon.value()))
                    .divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP)
                    .longValueExact();
        return new Eligibility(true, "可用", discount);
    }

    /**
     * 核销：`UNUSED` → `USED` 并绑定订单（条件更新，防并发重复核销）。
     */
    @Transactional
    public void consume(UUID couponId, UUID memberId, UUID orderId) {
        int updated = jdbcTemplate.update("""
                UPDATE member_coupon
                   SET status = 'USED', used_at = now(), order_id = ?
                 WHERE id = ? AND member_id = ? AND status = 'UNUSED' AND expires_at > now()
                """, orderId, couponId, memberId);
        if (updated != 1) {
            throw ApiException.conflict("COUPON_NOT_CONSUMABLE",
                    "券不可核销（已使用/已过期/不属于该会员）: " + couponId);
        }
        auditEvents.record(new com.fullmall.platform.audit.AuditEventService.AuditEvent(
                "COUPON_REDEEMED", "member:" + memberId, "coupon", couponId.toString(),
                "订单 " + orderId, null));
    }

    /**
     * 订单退款/取消时退券（幂等）。
     *
     * <p>未过期 → 恢复 `UNUSED`（可以再用）；已过期 → `EXPIRED`（不假装还能用）。
     * 两种情况都清空 `order_id` 与 `used_at`，满足 DB 上"USED 必须带 used_at"的约束。
     *
     * @return 被退回的券数量
     */
    @Transactional
    public int returnForOrder(UUID orderId) {
        int returned = jdbcTemplate.update("""
                UPDATE member_coupon
                   SET status = 'UNUSED', used_at = NULL, order_id = NULL
                 WHERE order_id = ? AND status = 'USED' AND expires_at > now()
                """, orderId);
        int expired = jdbcTemplate.update("""
                UPDATE member_coupon
                   SET status = 'EXPIRED', used_at = NULL, order_id = NULL
                 WHERE order_id = ? AND status = 'USED' AND expires_at <= now()
                """, orderId);
        return returned + expired;
    }

    /** 后台：券模板列表（含已发/总量）。 */
    public List<TemplateView> listTemplates(int limit) {
        if (limit <= 0 || limit > 200) {
            throw ApiException.badRequest("INVALID_LIMIT", "查询条数必须在 1..200，实际=" + limit);
        }
        return jdbcTemplate.query("""
                SELECT id, code, title, type, value, min_order_amount, total_quantity,
                       issued_quantity, per_member_limit, validity_days, status,
                       scope, scope_ids
                  FROM coupon_template ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> new TemplateView(
                rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("title"),
                rs.getString("type"), rs.getLong("value"), rs.getLong("min_order_amount"),
                rs.getInt("total_quantity"), rs.getInt("issued_quantity"),
                rs.getInt("per_member_limit"), rs.getInt("validity_days"), rs.getString("status")),
                limit);
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
        if (code == null || !code.matches(com.fullmall.platform.validation.ValidationRules.COUPON_CODE_PATTERN)) {
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

    // 券码是顾客出示给门店核销的凭据，必须不可猜：12 位、字符表去掉易混字符。
    // 这里不查重——唯一性由 member_coupon.code 的 UNIQUE 约束保证，撞码会让领券事务整体回滚（宁可失败也不发重码）。
    private String randomCode() {
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private record EligibilityRow(String status, Instant expiresAt, String type, long value,
                                  long minOrderAmount) {
    }

    private record TemplateRow(UUID id, String type, long value, int totalQuantity, int issuedQuantity,
                               int perMemberLimit, int validityDays, Instant startsAt, Instant endsAt,
                               String status) {
    }

    /** 单券叠加偏好；NULL 表示跟随全局配置（C2）。 */
    public Boolean stackableOrNull(UUID couponId) {
        var rows = jdbcTemplate.query("""
                SELECT t.stackable FROM coupon_template t
                  JOIN member_coupon c ON c.template_id = t.id WHERE c.id = ?
                """, (rs, rowNum) -> (Boolean) rs.getObject("stackable"), couponId);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
