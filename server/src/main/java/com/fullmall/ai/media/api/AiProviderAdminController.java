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
                                         extra, enabled, priority)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?)
                ON CONFLICT (code) DO UPDATE
                   SET kind = EXCLUDED.kind, name = EXCLUDED.name, protocol = EXCLUDED.protocol,
                       endpoint = EXCLUDED.endpoint,
                       api_key = COALESCE(EXCLUDED.api_key, ai_provider.api_key),
                       model = EXCLUDED.model, extra = EXCLUDED.extra,
                       enabled = EXCLUDED.enabled, priority = EXCLUDED.priority
                """, UUID.randomUUID(), body.kind().name(), code.trim(),
                isBlank(body.name()) ? code.trim() : body.name().trim(), body.protocol().trim(),
                body.endpoint().trim(), encrypted, body.model().trim(),
                com.fullmall.platform.json.Json.write(body.extra() == null ? Map.of() : body.extra()),
                body.enabled() == null || body.enabled(),
                body.priority() == null ? 100 : body.priority());
        return Map.of("code", code.trim(), "updated", true);
    }

    @org.springframework.web.bind.annotation.GetMapping
    public List<Map<String, Object>> list() {
        return jdbc.query("""
                SELECT code, kind, name, protocol, model, enabled, priority,
                       (api_key IS NOT NULL) AS has_key
                  FROM ai_provider ORDER BY kind, priority, code
                """, (rs, rowNum) -> Map.of(
                "code", rs.getString("code"),
                "kind", rs.getString("kind"),
                "name", rs.getString("name"),
                "protocol", rs.getString("protocol"),
                "model", rs.getString("model"),
                "enabled", rs.getBoolean("enabled"),
                "priority", rs.getInt("priority"),
                "hasKey", rs.getBoolean("has_key")));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
