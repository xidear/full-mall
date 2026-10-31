package com.fullmall.ai.media.api;

import com.fullmall.ai.media.MediaKind;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.secret.SecretCipher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** AI 供应商配置：API Key 加密落库，永不回显。 */
@RestController
@RequestMapping("/admin/v1/ai/providers")
@PreAuthorize("hasRole('ADMIN')")
public class AiProviderAdminController {

    private final JdbcTemplate jdbc;
    private final SecretCipher cipher;

    public AiProviderAdminController(JdbcTemplate jdbc, SecretCipher cipher) {
        this.jdbc = jdbc;
        this.cipher = cipher;
    }

    public record UpsertRequest(MediaKind kind, String name, String protocol, String endpoint,
                                Boolean vision,
                                String apiKey, String model, Map<String, Object> extra,
                                Boolean enabled, Integer priority) {
    }

    @PutMapping("/{code}")
    public Map<String, Object> upsert(@org.springframework.web.bind.annotation.PathVariable String code,
                                      @RequestBody UpsertRequest body) {
        if (body.kind() == null || isBlank(body.protocol()) || isBlank(body.endpoint())
                || isBlank(body.model())) {
            throw ApiException.badRequest("AI_PROVIDER_FIELDS_REQUIRED",
                    "kind/protocol/endpoint/model 必填");
        }
        String encrypted = isBlank(body.apiKey()) ? null : cipher.encrypt(body.apiKey().trim());
        jdbc.update("""
                INSERT INTO ai_provider (id, kind, code, name, protocol, endpoint, api_key, model,
                                         extra, enabled, priority, vision)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?)
                ON CONFLICT (code) DO UPDATE
                   SET kind = EXCLUDED.kind, name = EXCLUDED.name, protocol = EXCLUDED.protocol,
                       endpoint = EXCLUDED.endpoint,
                       api_key = COALESCE(EXCLUDED.api_key, ai_provider.api_key),
                       model = EXCLUDED.model, extra = EXCLUDED.extra,
                       enabled = EXCLUDED.enabled, priority = EXCLUDED.priority,
                       vision = EXCLUDED.vision
                """, UUID.randomUUID(), body.kind().name(), code.trim(),
                isBlank(body.name()) ? code.trim() : body.name().trim(), body.protocol().trim(),
                body.endpoint().trim(), encrypted, body.model().trim(),
                com.fullmall.platform.json.Json.write(body.extra() == null ? Map.of() : body.extra()),
                body.enabled() == null || body.enabled(),
                body.priority() == null ? 100 : body.priority(),
                body.vision() != null && body.vision());
        return Map.of("code", code.trim(), "updated", true);
    }

    @org.springframework.web.bind.annotation.GetMapping
    public List<Map<String, Object>> list() {
        return jdbc.query("""
                SELECT code, kind, name, protocol, endpoint, model, enabled, priority, vision,
                       (api_key IS NOT NULL) AS has_key
                  FROM ai_provider ORDER BY kind, priority, code
                """, (rs, rowNum) -> Map.of(
                "code", rs.getString("code"),
                "kind", rs.getString("kind"),
                "name", rs.getString("name"),
                "protocol", rs.getString("protocol"),
                "endpoint", rs.getString("endpoint"),
                "model", rs.getString("model"),
                "enabled", rs.getBoolean("enabled"),
                "priority", rs.getInt("priority"),
                "vision", rs.getBoolean("vision"),
                "hasKey", rs.getBoolean("has_key")));
    }

    /** 视觉模型是否可用（前端决定「AI 识别图片」按钮是否可点）。 */
    @org.springframework.web.bind.annotation.GetMapping("/vision")
    public Map<String, Object> vision() {
        var rows = jdbc.query("""
                SELECT code, model, endpoint FROM ai_provider
                 WHERE enabled AND vision ORDER BY priority, code LIMIT 1
                """, (rs, rowNum) -> Map.of(
                "code", rs.getString("code"),
                "model", rs.getString("model"),
                "endpoint", rs.getString("endpoint")));
        if (rows.isEmpty()) {
            return Map.of("configured", false, "hint",
                    "未配置视觉模型：在「AI 供应商」里把某个支持图片输入的供应商标记为 vision=true"
                            + "（远端公共模型服务或本地 CPU 推理服务均可），不影响其它功能");
        }
        return Map.of("configured", true, "provider", rows.get(0));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
