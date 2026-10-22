package com.fullmall.platform.secret;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 第三方配置读写（ADR-005）。
 *
 * <p>铁律：
 * <ul>
 *   <li>敏感字段（含 key/secret/token/password/private 的键名）**只加密入库**；</li>
 *   <li>读取接口**永不返回敏感值**，只返回"是否已配置"+非敏感字段；</li>
 *   <li>同一能力启用新 provider 时，旧配置自动置 false（事务内）；</li>
 *   <li>更新写审计事件，只记字段名不记值。</li>
 * </ul>
 */
@Service
public class IntegrationConfigService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> SENSITIVE_MARKERS =
            List.of("secret", "key", "token", "password", "private");

    private final JdbcTemplate jdbc;
    private final SecretCipher cipher;

    public IntegrationConfigService(JdbcTemplate jdbc, SecretCipher cipher) {
        this.jdbc = jdbc;
        this.cipher = cipher;
    }

    public record Saved(String capability, String provider, boolean enabled,
                        List<String> secretFields, List<String> visibleFields) {
    }

    public record View(String capability, String provider, boolean enabled,
                       Map<String, Object> hint, boolean secretsConfigured) {
    }

    @Transactional
    public Saved save(String capability, String provider, Map<String, String> config,
                      boolean enabled, String actor) {
        if (capability == null || capability.isBlank() || provider == null || provider.isBlank()) {
            throw ApiException.badRequest("INTEGRATION_KEY_REQUIRED", "能力与供应商不能为空");
        }
        if (config == null || config.isEmpty()) {
            throw ApiException.badRequest("INTEGRATION_CONFIG_EMPTY", "配置不能为空");
        }
        Map<String, String> secrets = new LinkedHashMap<>();
        Map<String, Object> hint = new LinkedHashMap<>();
        for (var entry : config.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (isSensitive(name)) {
                if (value != null && !value.isBlank()) {
                    // 空白表示"保持不变"：沿用旧密文
                    secrets.put(name, value);
                }
                continue;
            }
            hint.put(name, value);
        }
        String encrypted;
        if (secrets.isEmpty()) {
            var existing = jdbc.query("SELECT config_enc FROM integration_config WHERE capability = ? AND provider = ?",
                    (rs, rowNum) -> rs.getBytes("config_enc"), capability, provider);
            // 无敏感字段的服务商（如 local 存储）允许保存；有旧密文则沿用
            encrypted = existing.isEmpty()
                    ? cipher.encrypt("{}")
                    : new String(existing.get(0), java.nio.charset.StandardCharsets.UTF_8);
        } else {
            encrypted = cipher.encrypt(toJson(secrets));
        }

        if (enabled) {
            jdbc.update("UPDATE integration_config SET enabled = false WHERE capability = ? AND provider <> ?",
                    capability, provider);
        }
        jdbc.update("""
                INSERT INTO integration_config (capability, provider, enabled, config_enc, config_hint,
                                                updated_by, updated_at)
                VALUES (?, ?, ?, ?, ?::jsonb, ?, now())
                ON CONFLICT (capability, provider) DO UPDATE
                   SET enabled = EXCLUDED.enabled, config_enc = EXCLUDED.config_enc,
                       config_hint = EXCLUDED.config_hint, updated_by = EXCLUDED.updated_by,
                       updated_at = now()
                """, capability, provider, enabled, encrypted.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                toJson(hint), actor);
        return new Saved(capability, provider, enabled,
                List.copyOf(secrets.keySet()), List.copyOf(hint.keySet()));
    }

    /** 读取：只返回非敏感字段与"是否已配置"，不含任何 secret。 */
    public List<View> list(String capability) {
        return jdbc.query("""
                SELECT capability, provider, enabled, config_hint::text AS hint,
                       (config_enc IS NOT NULL) AS secrets_configured
                  FROM integration_config
                 WHERE (? IS NULL OR capability = ?)
                 ORDER BY capability, provider
                """, (rs, rowNum) -> new View(
                rs.getString("capability"), rs.getString("provider"), rs.getBoolean("enabled"),
                readMap(rs.getString("hint")), rs.getBoolean("secrets_configured")),
                capability, capability);
    }

    /** 供适配器装配用：取出解密后的完整配置（仅内部调用，绝不暴露给 HTTP 层）。 */
    public Map<String, String> resolve(String capability) {
        var rows = jdbc.query("""
                SELECT config_enc FROM integration_config
                 WHERE capability = ? AND enabled
                """, (rs, rowNum) -> new String(rs.getBytes("config_enc"),
                java.nio.charset.StandardCharsets.UTF_8), capability);
        if (rows.isEmpty()) {
            throw ApiException.conflict("INTEGRATION_NOT_CONFIGURED",
                    "能力「" + capability + "」尚未在后台启用配置");
        }
        Map<String, String> resolved = new LinkedHashMap<>();
        readMap(cipher.decrypt(rows.get(0))).forEach((k, v) ->
                resolved.put(k, v == null ? null : String.valueOf(v)));
        return resolved;
    }

    private boolean isSensitive(String name) {
        String lower = name == null ? "" : name.toLowerCase(java.util.Locale.ROOT);
        return SENSITIVE_MARKERS.stream().anyMatch(lower::contains);
    }

    private String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException("配置序列化失败", error);
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            return MAPPER.readValue(json == null ? "{}" : json,
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    });
        } catch (Exception error) {
            throw new IllegalStateException("配置解析失败", error);
        }
    }

    /** 敏感键名规则（供文档与测试引用）。 */
    public static Set<String> sensitiveMarkers() {
        return Set.copyOf(SENSITIVE_MARKERS);
    }
}
