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

        ObjectNode rules = MAPPER.createObjectNode();
        rules.put("thresholdAmount", command.thresholdAmount());
        rules.put("value", command.value());
        rules.put("scope", command.scope());
        rules.put("channel", command.channel());
        var ids = rules.putArray("scopeIds");
        if (command.scopeIds() != null) {
            command.scopeIds().forEach(id -> ids.add(id.toString()));
        }

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
