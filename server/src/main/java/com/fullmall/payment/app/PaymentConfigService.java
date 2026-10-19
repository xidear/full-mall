package com.fullmall.payment.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 支付渠道配置（PAY-002）。
 *
 * <p>两种模式（见 `docs/PAYMENT_PROVIDER_MODES_AND_CONFIG_UI.md`）：
 * <ul>
 *   <li>`DIRECT`：我方直接是商户，`sp*` 与 `sub*` 必须为空；</li>
 *   <li>`SUB_MERCHANT`：**上游服务商** 提供 `spMchId`，**我方商城是唯一子商户** `subMchId`，两者必填。</li>
 * </ul>
 *
 * <p>配置可编辑（换密钥/换服务商），因此每次编辑生成**新的不可变版本**；
 * 支付记录引用版本，退款按版本路由。返回给后台的敏感标识一律掩码。
 */
@Service
public class PaymentConfigService {

    private static final List<String> CHANNELS = List.of("WECHAT", "ALIPAY", "UNIONPAY", "AGGREGATOR");
    private static final List<String> MODES = List.of("DIRECT", "SUB_MERCHANT");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private final AuditEventService auditEvents;

    public PaymentConfigService(JdbcTemplate jdbcTemplate, AuditEventService auditEvents) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditEvents = auditEvents;
    }

    public record ConfigView(UUID id, String channel, String mode, String marketCode,
                             String displayName, boolean active, int version,
                             String spMchIdMasked, String subMchIdMasked) {
    }

    public record ConfigInput(String channel, String mode, String marketCode, String displayName,
                              String spName, String spMchId, String spAppId,
                              String subMchId, String subAppId,
                              String mchId, String appId, String callbackUrl) {
    }

    @Transactional
    public UUID create(ConfigInput input, String actor) {
        requireActor(actor);
        validate(input);

        Long activeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM payment_provider_config
                 WHERE channel = ? AND market_code = ? AND active
                """, Long.class, input.channel(), input.marketCode());
        if (activeCount == null) {
            throw new IllegalStateException("支付配置计数返回 null");
        }
        if (activeCount > 0) {
            throw ApiException.conflict("PAYMENT_CONFIG_ALREADY_ACTIVE",
                    "该渠道与市场已有生效配置，请先停用再新增（换服务商需保留旧配置用于历史退款）");
        }

        UUID configId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO payment_provider_config (id, channel, mode, market_code, display_name,
                                                     active, current_version)
                VALUES (?, ?, ?, ?, ?, TRUE, 1)
                """, configId, input.channel(), input.mode(), input.marketCode(),
                input.displayName() == null || input.displayName().isBlank()
                        ? input.channel() : input.displayName().trim());
        insertVersion(configId, 1, input, actor);
        audit(configId, "PAYMENT_CONFIG_CREATED", actor, input.channel() + "/" + input.mode());
        return configId;
    }

    /** 编辑配置 = 生成新版本；历史支付仍指向旧版本。 */
    @Transactional
    public int update(UUID configId, ConfigInput input, String actor) {
        requireActor(actor);
        validate(input);
        Integer current = jdbcTemplate.queryForObject(
                "SELECT current_version FROM payment_provider_config WHERE id = ?", Integer.class, configId);
        if (current == null) {
            throw ApiException.notFound("PAYMENT_CONFIG_NOT_FOUND", "支付配置不存在: " + configId);
        }
        int next = current + 1;
        insertVersion(configId, next, input, actor);
        int updated = jdbcTemplate.update("""
                UPDATE payment_provider_config
                   SET mode = ?, display_name = ?, current_version = ?, updated_at = now()
                 WHERE id = ?
                """, input.mode(),
                input.displayName() == null || input.displayName().isBlank() ? input.channel() : input.displayName().trim(),
                next, configId);
        if (updated != 1) {
            throw new IllegalStateException("支付配置更新失败: " + configId);
        }
        audit(configId, "PAYMENT_CONFIG_UPDATED", actor, "version=" + next);
        return next;
    }

    @Transactional
    public void deactivate(UUID configId, String actor) {
        requireActor(actor);
        int updated = jdbcTemplate.update("""
                UPDATE payment_provider_config SET active = FALSE, updated_at = now()
                 WHERE id = ? AND active
                """, configId);
        if (updated != 1) {
            throw ApiException.conflict("PAYMENT_CONFIG_NOT_ACTIVE", "配置不存在或已停用: " + configId);
        }
        audit(configId, "PAYMENT_CONFIG_DEACTIVATED", actor, null);
    }

    public List<ConfigView> list() {
        return jdbcTemplate.query("""
                SELECT c.id, c.channel, c.mode, c.market_code, c.display_name, c.active, c.current_version,
                       COALESCE(v.settings->>'spMchId', '') AS sp_mch_id,
                       COALESCE(v.settings->>'subMchId', '') AS sub_mch_id
                  FROM payment_provider_config c
                  LEFT JOIN payment_provider_config_version v
                         ON v.config_id = c.id AND v.version_no = c.current_version
                 ORDER BY c.channel, c.market_code
                """, (rs, rowNum) -> new ConfigView(
                rs.getObject("id", UUID.class),
                rs.getString("channel"),
                rs.getString("mode"),
                rs.getString("market_code"),
                rs.getString("display_name"),
                rs.getBoolean("active"),
                rs.getInt("current_version"),
                mask(rs.getString("sp_mch_id")),
                mask(rs.getString("sub_mch_id"))));
    }

    /** 掩码：保留前 3 后 4，避免后台整段泄露商户号。 */
    static String mask(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= 7) {
            return "*".repeat(value.length());
        }
        return value.substring(0, 3) + "*".repeat(value.length() - 7)
                + value.substring(value.length() - 4);
    }

    private void validate(ConfigInput input) {
        if (input.channel() == null || !CHANNELS.contains(input.channel())) {
            throw ApiException.badRequest("INVALID_PAYMENT_CHANNEL", "未知支付渠道: " + input.channel());
        }
        if (input.mode() == null || !MODES.contains(input.mode())) {
            throw ApiException.badRequest("INVALID_PAYMENT_MODE",
                    "mode 只能是 DIRECT 或 SUB_MERCHANT，实际=" + input.mode());
        }
        if (input.marketCode() == null || input.marketCode().isBlank()) {
            throw ApiException.badRequest("MARKET_REQUIRED", "必须指定市场");
        }
        boolean hasSp = notBlank(input.spMchId()) || notBlank(input.spAppId());
        boolean hasSub = notBlank(input.subMchId()) || notBlank(input.subAppId());

        if ("DIRECT".equals(input.mode())) {
            if (hasSp || hasSub) {
                throw ApiException.badRequest("DIRECT_MODE_FORBIDS_SP_FIELDS",
                        "直连模式下不能配置服务商/子商户字段");
            }
            if (!notBlank(input.mchId())) {
                throw ApiException.badRequest("MCH_ID_REQUIRED", "直连模式必须填写我方商户号");
            }
        } else {
            if (!notBlank(input.spMchId())) {
                throw ApiException.badRequest("SP_MCH_ID_REQUIRED",
                        "服务商模式必须填写上游服务商商户号 spMchId");
            }
            if (!notBlank(input.subMchId())) {
                throw ApiException.badRequest("SUB_MCH_ID_REQUIRED",
                        "服务商模式必须填写我方子商户号 subMchId（整个商城一个）");
            }
        }
    }

    private void insertVersion(UUID configId, int versionNo, ConfigInput input, String actor) {
        String settings;
        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("spName", nullSafe(input.spName()));
            payload.put("spMchId", nullSafe(input.spMchId()));
            payload.put("spAppId", nullSafe(input.spAppId()));
            payload.put("subMchId", nullSafe(input.subMchId()));
            payload.put("subAppId", nullSafe(input.subAppId()));
            payload.put("mchId", nullSafe(input.mchId()));
            payload.put("appId", nullSafe(input.appId()));
            payload.put("callbackUrl", nullSafe(input.callbackUrl()));
            settings = MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("支付配置序列化失败", error);
        }
        int inserted = jdbcTemplate.update("""
                INSERT INTO payment_provider_config_version (id, config_id, version_no, channel, mode,
                                                            settings, created_by)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
                """, UUID.randomUUID(), configId, versionNo, input.channel(), input.mode(), settings, actor);
        if (inserted != 1) {
            throw new IllegalStateException("支付配置版本写入失败: config=" + configId + ", v=" + versionNo);
        }
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw ApiException.badRequest("ACTOR_REQUIRED", "支付配置变更必须记录操作人");
        }
    }

    private void audit(UUID configId, String type, String actor, String detail) {
        auditEvents.record(new AuditEventService.AuditEvent(
                type, actor, "payment_provider_config", configId.toString(), detail, null));
    }
}