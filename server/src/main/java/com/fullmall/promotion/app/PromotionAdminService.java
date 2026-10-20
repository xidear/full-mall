package com.fullmall.promotion.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 后台促销规则构建（PRO-002 的一部分）。
 *
 * <p>运营通过**结构化表单**创建规则，JSON 由服务端生成——**不允许手填 JSON**。
 * 校验：类型/门槛/优惠值/渠道/范围/时间窗/优先级；范围 ID 必须真实存在（SKU/类目/品牌）。
 */
@Service
public class PromotionAdminService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> RULE_TYPES = List.of("FULL_REDUCTION", "FULL_DISCOUNT");
    private static final List<String> SCOPES = List.of("ALL", "SKU", "CATEGORY", "BRAND");
    private static final List<String> CHANNELS = List.of("ONLINE", "POS", "PICKUP");

    private final JdbcTemplate jdbcTemplate;
    private final AuditEventService auditEvents;

    public PromotionAdminService(JdbcTemplate jdbcTemplate, AuditEventService auditEvents) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditEvents = auditEvents;
    }

    public record Command(String code, String title, String type, long thresholdAmount, long value,
                          String scope, List<UUID> scopeIds, String channel,
                          Instant startsAt, Instant endsAt, int priority) {
    }

    @Transactional
    public UUID create(Command command, String actor) {
        requireActor(actor);
        validate(command);

        ObjectNode rules = buildRules(command);

        UUID id = UUID.randomUUID();
        int inserted = jdbcTemplate.update("""
                INSERT INTO promotion (id, code, type, title, status, starts_at, ends_at, rules, priority)
                VALUES (?, ?, ?, ?, 'DRAFT', ?, ?, CAST(? AS jsonb), ?)
                """, id, command.code(), command.type(), command.title(),
                Timestamp.from(command.startsAt()), Timestamp.from(command.endsAt()),
                rules.toString(), command.priority());
        if (inserted != 1) {
            throw new IllegalStateException("促销规则创建失败: " + command.code());
        }
        audit("PROMOTION_CREATED", id, actor, command.type() + "/" + command.scope());
        return id;
    }

    public record RuleView(UUID id, String code, String title, String type, String status,
                           long thresholdAmount, long value, String scope, String channel,
                           int priority, Instant startsAt, Instant endsAt) {
    }

    /** 范围可选项（SKU/类目/品牌），供后台选择器使用。 */
    public record ScopeOption(UUID id, String label, String extra) {
    }

    public List<ScopeOption> scopeOptions(String scope, int limit) {
        String normalized = scope == null ? "" : scope.trim().toUpperCase(Locale.ROOT);
        if (limit <= 0 || limit > 200) {
            throw ApiException.badRequest("INVALID_LIMIT", "查询条数必须在 1..200，实际=" + limit);
        }
        return switch (normalized) {
            case "SKU" -> jdbcTemplate.query("""
                    SELECT s.id, s.sku_code AS label, p.title AS extra
                      FROM sku s JOIN product p ON p.id = s.product_id
                     ORDER BY s.sku_code LIMIT ?
                    """, (rs, rowNum) -> new ScopeOption(
                    rs.getObject("id", UUID.class), rs.getString("label"), rs.getString("extra")), limit);
            case "CATEGORY" -> jdbcTemplate.query("""
                    SELECT id, name AS label, code AS extra FROM category ORDER BY name LIMIT ?
                    """, (rs, rowNum) -> new ScopeOption(
                    rs.getObject("id", UUID.class), rs.getString("label"), rs.getString("extra")), limit);
            case "BRAND" -> jdbcTemplate.query("""
                    SELECT id, name AS label, NULL AS extra FROM brand ORDER BY name LIMIT ?
                    """, (rs, rowNum) -> new ScopeOption(
                    rs.getObject("id", UUID.class), rs.getString("label"), rs.getString("extra")), limit);
            default -> throw ApiException.badRequest("INVALID_SCOPE",
                    "范围只能是 SKU / CATEGORY / BRAND");
        };
    }

    public List<RuleView> list(String status, int limit) {
        if (limit <= 0 || limit > 200) {
            throw ApiException.badRequest("INVALID_LIMIT", "查询条数必须在 1..200，实际=" + limit);
        }
        String filter = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        return jdbcTemplate.query("""
                SELECT id, code, title, type, status, priority, starts_at, ends_at,
                       COALESCE((rules->>'thresholdAmount')::bigint, 0) AS threshold_amount,
                       COALESCE((rules->>'value')::bigint, 0) AS rule_value,
                       COALESCE(rules->>'scope', 'ALL') AS scope,
                       COALESCE(rules->>'channel', 'ONLINE') AS channel
                  FROM promotion
                 WHERE type IN ('FULL_REDUCTION', 'FULL_DISCOUNT')
                   AND (?::text IS NULL OR status = ?::text)
                 ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> new RuleView(
                rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("title"),
                rs.getString("type"), rs.getString("status"),
                rs.getLong("threshold_amount"), rs.getLong("rule_value"),
                rs.getString("scope"), rs.getString("channel"), rs.getInt("priority"),
                rs.getTimestamp("starts_at").toInstant(), rs.getTimestamp("ends_at").toInstant()),
                filter, filter, limit);
    }

    /** 编辑仅允许草稿：已生效规则被静默改动会让"试算与结算"在用户眼皮下变化。 */
    @Transactional
    public void update(UUID id, Command command, String actor) {
        requireActor(actor);
        validate(command);
        String status = jdbcTemplate.queryForList(
                "SELECT status FROM promotion WHERE id = ?", String.class, id)
                .stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("PROMOTION_NOT_FOUND", "规则不存在: " + id));
        if (!"DRAFT".equals(status)) {
            throw ApiException.conflict("PROMOTION_NOT_EDITABLE",
                    "只有草稿状态的规则可以编辑，当前=" + status + "（改规则请先结束并新建）");
        }

        ObjectNode rules = buildRules(command);
        int updated = jdbcTemplate.update("""
                UPDATE promotion
                   SET code = ?, title = ?, type = ?, starts_at = ?, ends_at = ?,
                       rules = CAST(? AS jsonb), priority = ?
                 WHERE id = ? AND status = 'DRAFT'
                """, command.code(), command.title(), command.type(),
                Timestamp.from(command.startsAt()), Timestamp.from(command.endsAt()),
                rules.toString(), command.priority(), id);
        if (updated != 1) {
            throw new IllegalStateException("规则更新失败: " + id);
        }
        audit("PROMOTION_UPDATED", id, actor, command.type() + "/" + command.scope());
    }

    private ObjectNode buildRules(Command command) {
        ObjectNode rules = MAPPER.createObjectNode();
        rules.put("thresholdAmount", command.thresholdAmount());
        rules.put("value", command.value());
        rules.put("scope", command.scope() == null ? "ALL" : command.scope().toUpperCase(Locale.ROOT));
        rules.put("channel", command.channel() == null ? "ONLINE" : command.channel().toUpperCase(Locale.ROOT));
        var ids = rules.putArray("scopeIds");
        if (command.scopeIds() != null) {
            command.scopeIds().forEach(scopeId -> ids.add(scopeId.toString()));
        }
        return rules;
    }

    @Transactional
    public void activate(UUID id, String actor) {
        requireActor(actor);
        int updated = jdbcTemplate.update("""
                UPDATE promotion SET status = 'ACTIVE' WHERE id = ? AND status = 'DRAFT'
                """, id);
        if (updated != 1) {
            throw ApiException.conflict("PROMOTION_NOT_ACTIVATABLE",
                    "只有草稿状态可以启用: " + id);
        }
        audit("PROMOTION_ACTIVATED", id, actor, null);
    }

    @Transactional
    public void end(UUID id, String actor) {
        requireActor(actor);
        int updated = jdbcTemplate.update("""
                UPDATE promotion SET status = 'ENDED' WHERE id = ? AND status = 'ACTIVE'
                """, id);
        if (updated != 1) {
            throw ApiException.conflict("PROMOTION_NOT_ENDABLE", "只有进行中的促销可以结束: " + id);
        }
        audit("PROMOTION_ENDED", id, actor, null);
    }

    private void validate(Command command) {
        if (command.code() == null || !command.code().matches("^[A-Z0-9][A-Z0-9_-]{1,63}$")) {
            throw ApiException.badRequest("INVALID_PROMOTION_CODE",
                    "规则编码只能是大写字母/数字/-/_，长度 2..64");
        }
        if (command.title() == null || command.title().isBlank()) {
            throw ApiException.badRequest("TITLE_REQUIRED", "规则标题不能为空");
        }
        String type = command.type() == null ? "" : command.type().toUpperCase(Locale.ROOT);
        if (!RULE_TYPES.contains(type)) {
            throw ApiException.badRequest("INVALID_PROMOTION_TYPE",
                    "当前构建器只支持 FULL_REDUCTION / FULL_DISCOUNT");
        }
        if (command.thresholdAmount() <= 0) {
            throw ApiException.badRequest("INVALID_THRESHOLD", "促销门槛必须大于 0");
        }
        if ("FULL_REDUCTION".equals(type) && command.value() <= 0) {
            throw ApiException.badRequest("INVALID_DISCOUNT", "满减金额必须大于 0");
        }
        if ("FULL_DISCOUNT".equals(type) && (command.value() <= 0 || command.value() >= 100)) {
            throw ApiException.badRequest("INVALID_DISCOUNT", "满折百分比必须在 1..99");
        }
        String scope = command.scope() == null ? "ALL" : command.scope().toUpperCase(Locale.ROOT);
        if (!SCOPES.contains(scope)) {
            throw ApiException.badRequest("INVALID_SCOPE", "范围只能是 ALL / SKU / CATEGORY / BRAND");
        }
        if (!"ALL".equals(scope)) {
            if (command.scopeIds() == null || command.scopeIds().isEmpty()) {
                throw ApiException.badRequest("SCOPE_IDS_REQUIRED", "非全场规则必须指定范围 ID");
            }
            String table = switch (scope) {
                case "SKU" -> "sku";
                case "CATEGORY" -> "category";
                default -> "brand";
            };
            for (UUID scopeId : command.scopeIds()) {
                Integer exists = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, scopeId);
                if (exists == null || exists == 0) {
                    throw ApiException.badRequest("SCOPE_ID_NOT_FOUND",
                            "范围 ID 不存在: " + scope + "/" + scopeId);
                }
            }
        }
        String channel = command.channel() == null ? "ONLINE" : command.channel().toUpperCase(Locale.ROOT);
        if (!CHANNELS.contains(channel)) {
            throw ApiException.badRequest("INVALID_CHANNEL", "渠道只能是 ONLINE / POS / PICKUP");
        }
        if (command.startsAt() == null || command.endsAt() == null
                || !command.endsAt().isAfter(command.startsAt())) {
            throw ApiException.badRequest("INVALID_WINDOW", "结束时间必须晚于开始时间");
        }
        if (command.priority() < 0) {
            throw ApiException.badRequest("INVALID_PRIORITY", "优先级不能为负");
        }
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw ApiException.badRequest("ACTOR_REQUIRED", "必须记录操作人");
        }
    }

    private void audit(String type, UUID id, String actor, String detail) {
        auditEvents.record(new AuditEventService.AuditEvent(
                type, actor, "promotion", id.toString(), detail, null));
    }
}
