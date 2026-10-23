package com.fullmall.analytics.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * AI 受控写操作（N3）：草稿 → 双人审批 → 执行。
 * 同一人不能签两次；只有 APPROVED 可执行；动作白名单。
 */
@Service
public class AiActionDraftService {

    private static final ObjectMapper MAPPER = com.fullmall.platform.json.Json.mapper();
    private static final Set<String> ACTIONS = Set.of("sku.price.set", "product.status.set", "coupon.grant");

    private final JdbcTemplate jdbc;

    public AiActionDraftService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Draft(UUID id, String actionType, String status, String requestedBy,
                        String approvedBy1, String approvedBy2) {
    }

    @Transactional
    public Draft create(String actionType, Map<String, Object> payload, String requestedBy) {
        String action = actionType == null ? "" : actionType.trim();
        if (!ACTIONS.contains(action)) {
            throw ApiException.badRequest("AI_ACTION_UNSUPPORTED",
                    "不支持的动作：" + actionType + "（允许 " + ACTIONS + "）");
        }
        if (requestedBy == null || requestedBy.isBlank()) {
            throw ApiException.badRequest("AI_ACTION_REQUESTER_REQUIRED", "必须记录发起人");
        }
        String json;
        try {
            json = MAPPER.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (Exception error) {
            throw ApiException.badRequest("AI_ACTION_PAYLOAD_INVALID", "payload 无法序列化");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO ai_action_draft (id, action_type, payload, status, requested_by)
                VALUES (?, ?, ?::jsonb, 'PENDING_APPROVAL', ?)
                """, id, action, json, requestedBy);
        return find(id);
    }

    /** 供模型提议使用：动作/参数不合法时转成业务异常（由上层给出明确错误）。 */
    @Transactional
    public Draft createAllowingUnsupported(Map.Entry<String, Map<String, Object>> parsed,
                                           String requestedBy) {
        return create(parsed.getKey(), parsed.getValue(), requestedBy);
    }

    @Transactional
    public Draft approve(UUID id, String actor) {
        requireActor(actor);
        Draft draft = find(id);
        if (!"PENDING_APPROVAL".equals(draft.status())) {
            throw ApiException.conflict("AI_ACTION_NOT_PENDING", "当前状态不可审批：" + draft.status());
        }
        if (actor.equals(draft.approvedBy1())) {
            throw ApiException.conflict("AI_ACTION_SAME_APPROVER", "同一人不能签两次，需要第二个管理员");
        }
        if (draft.approvedBy1() == null) {
            jdbc.update("UPDATE ai_action_draft SET approved_by_1 = ? WHERE id = ?", actor, id);
        } else {
            jdbc.update("UPDATE ai_action_draft SET approved_by_2 = ?, status = 'APPROVED' WHERE id = ?",
                    actor, id);
        }
        return find(id);
    }

    @Transactional
    public Draft reject(UUID id, String actor, String reason) {
        requireActor(actor);
        Draft draft = find(id);
        if (!"PENDING_APPROVAL".equals(draft.status())) {
            throw ApiException.conflict("AI_ACTION_NOT_PENDING", "当前状态不可驳回：" + draft.status());
        }
        jdbc.update("""
                UPDATE ai_action_draft SET status = 'REJECTED', rejected_by = ?, reject_reason = ?
                 WHERE id = ?
                """, actor, reason, id);
        return find(id);
    }

    @Transactional
    public Draft execute(UUID id, String actor) {
        requireActor(actor);
        Draft draft = find(id);
        if (!"APPROVED".equals(draft.status())) {
            throw ApiException.conflict("AI_ACTION_NOT_APPROVED",
                    "只有双签通过后才能执行，当前=" + draft.status());
        }
        Map<String, Object> payload = payloadOf(id);
        if ("coupon.grant".equals(draft.actionType())) {
            String memberNo = String.valueOf(payload.getOrDefault("memberId", "")).trim();
            String templateNo = String.valueOf(payload.getOrDefault("templateId", "")).trim();
            if (memberNo.isEmpty() || templateNo.isEmpty()) {
                throw ApiException.badRequest("AI_ACTION_PAYLOAD_INVALID",
                        "coupon.grant 需要 memberId 与 templateId");
            }
            Integer remaining = jdbc.queryForObject("""
                    SELECT total_quantity - issued_quantity FROM coupon_template
                     WHERE id = ?::uuid AND ends_at > now()
                    """, Integer.class, templateNo);
            if (remaining == null) {
                throw ApiException.notFound("COUPON_TEMPLATE_NOT_FOUND",
                        "券模板不存在或已过期：" + templateNo);
            }
            if (remaining <= 0) {
                throw ApiException.conflict("COUPON_SOLD_OUT", "券已发完：" + templateNo);
            }
            String code = "AI-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 10).toUpperCase(java.util.Locale.ROOT);
            jdbc.update("""
                    INSERT INTO member_coupon (id, template_id, member_id, code, status, expires_at)
                    SELECT ?, ?, ?, ?, 'UNUSED', now() + (validity_days || ' days')::interval
                      FROM coupon_template WHERE id = ?::uuid
                    """, UUID.randomUUID(), java.util.UUID.fromString(templateNo),
                    java.util.UUID.fromString(memberNo), code, templateNo);
            jdbc.update("""
                    UPDATE coupon_template SET issued_quantity = issued_quantity + 1 WHERE id = ?::uuid
                    """, templateNo);
        }
        if ("product.status.set".equals(draft.actionType())) {
            String productNo = String.valueOf(payload.getOrDefault("productId", "")).trim();
            String status = String.valueOf(payload.getOrDefault("status", "")).trim().toUpperCase(java.util.Locale.ROOT);
            if (productNo.isEmpty() || !java.util.List.of("ACTIVE", "ARCHIVED", "DRAFT").contains(status)) {
                throw ApiException.badRequest("AI_ACTION_PAYLOAD_INVALID",
                        "product.status.set 需要 productId 与 status(ACTIVE/ARCHIVED/DRAFT)");
            }
            int updated = jdbc.update("UPDATE product SET status = ? WHERE id = ?::uuid",
                    status, productNo);
            if (updated != 1) {
                throw ApiException.notFound("PRODUCT_NOT_FOUND", "商品不存在：" + productNo);
            }
        }
        if ("sku.price.set".equals(draft.actionType())) {
            String skuCode = String.valueOf(payload.getOrDefault("skuCode", "")).trim();
            Object price = payload.get("priceAmount");
            if (skuCode.isEmpty() || !(price instanceof Number)) {
                throw ApiException.badRequest("AI_ACTION_PAYLOAD_INVALID",
                        "sku.price.set 需要 skuCode 与 priceAmount（分）");
            }
            long amount = ((Number) price).longValue();
            if (amount <= 0) {
                throw ApiException.badRequest("AI_ACTION_PRICE_INVALID", "价格必须大于 0");
            }
            int updated = jdbc.update("UPDATE sku SET price_amount = ? WHERE sku_code = ?", amount, skuCode);
            if (updated != 1) {
                throw ApiException.notFound("SKU_NOT_FOUND", "SKU 不存在：" + skuCode);
            }
        }
        jdbc.update("UPDATE ai_action_draft SET status = 'EXECUTED', executed_at = now() WHERE id = ?", id);
        return find(id);
    }

    public List<Map<String, Object>> pending(int limit) {
        return jdbc.query("""
                SELECT id, action_type, status, requested_by, approved_by_1, approved_by_2, created_at
                  FROM ai_action_draft
                 WHERE status IN ('PENDING_APPROVAL', 'APPROVED')
                 ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> Map.of(
                "id", rs.getObject("id", UUID.class).toString(),
                "actionType", rs.getString("action_type"),
                "status", rs.getString("status"),
                "requestedBy", rs.getString("requested_by"),
                "approvedBy1", rs.getString("approved_by_1") == null ? "" : rs.getString("approved_by_1"),
                "approvedBy2", rs.getString("approved_by_2") == null ? "" : rs.getString("approved_by_2"),
                "createdAt", rs.getTimestamp("created_at").toInstant().toString()),
                Math.min(Math.max(limit, 1), 100));
    }

    private Draft find(UUID id) {
        var rows = jdbc.query("""
                SELECT id, action_type, status, requested_by, approved_by_1, approved_by_2
                  FROM ai_action_draft WHERE id = ?
                """, (rs, rowNum) -> new Draft(
                rs.getObject("id", UUID.class), rs.getString("action_type"),
                rs.getString("status"), rs.getString("requested_by"),
                rs.getString("approved_by_1"), rs.getString("approved_by_2")), id);
        if (rows.isEmpty()) {
            throw ApiException.notFound("AI_ACTION_NOT_FOUND", "草稿不存在：" + id);
        }
        return rows.get(0);
    }

    private Map<String, Object> payloadOf(UUID id) {
        String json = jdbc.queryForObject(
                "SELECT payload::text FROM ai_action_draft WHERE id = ?", String.class, id);
        try {
            return MAPPER.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<
                    LinkedHashMap<String, Object>>() {
            });
        } catch (Exception error) {
            throw new IllegalStateException("草稿载荷解析失败", error);
        }
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw ApiException.badRequest("AI_ACTION_ACTOR_REQUIRED", "必须记录操作人");
        }
    }
}
