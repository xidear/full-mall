package com.fullmall.ai.media;

import com.fullmall.ai.media.adapter.GeneratedMedia;
import com.fullmall.ai.media.adapter.MediaGenerationAdapter;
import com.fullmall.platform.asset.AssetStorage;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.json.Json;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * AI 素材生产（图/视频/文案）：任务入队 → 异步执行 → 结果入素材库。
 *
 * <p>定位（明确）：**只做内容生产**，不参与商品上下架/改价这类本来点两下就完成的运营动作。
 *
 * <p>规则：
 * <ul>
 *   <li>供应商可同类多家，按 priority 选择，也可显式指定 code；</li>
 *   <li>提示词与导图都作为资产留痕（`ai_prompt_history` / `source_asset_ids`）；</li>
 *   <li>失败必须写 `error`，任务状态置 FAILED，不静默丢弃。</li>
 * </ul>
 */
@Service
public class AiMediaService {

    private final JdbcTemplate jdbc;
    private final AssetStorage storage;
    private final com.fullmall.platform.secret.SecretCipher cipher;
    private final Map<String, MediaGenerationAdapter> adapters = new LinkedHashMap<>();

    public AiMediaService(JdbcTemplate jdbc, AssetStorage storage,
                          com.fullmall.platform.secret.SecretCipher cipher,
                          List<MediaGenerationAdapter> adapterBeans) {
        this.jdbc = jdbc;
        this.storage = storage;
        this.cipher = cipher;
        for (MediaGenerationAdapter adapter : adapterBeans) {
            adapters.put(adapter.protocol(), adapter);
        }
    }

    /** 单张参考图及其角色：REFERENCE 通用参考 / SUBJECT 主体(角色) / FIRST_FRAME / LAST_FRAME。 */
    public record ImageRef(UUID assetId, String role) {
    }

    public record CreateJob(MediaKind kind, String providerCode, UUID productId, UUID skuId,
                            String purpose, String prompt, String negative,
                            Map<String, Object> params, List<UUID> sourceAssetIds,
                            List<ImageRef> images) {
        /** 兼容旧调用：只有素材 id 时按 REFERENCE 处理。 */
        public CreateJob(MediaKind kind, String providerCode, UUID productId, UUID skuId,
                         String purpose, String prompt, String negative,
                         Map<String, Object> params, List<UUID> sourceAssetIds) {
            this(kind, providerCode, productId, skuId, purpose, prompt, negative, params,
                    sourceAssetIds, List.of());
        }
    }

    public List<Map<String, Object>> providers(MediaKind kind) {
        return jdbc.query("""
                SELECT id, kind, code, name, protocol, model, enabled, priority
                  FROM ai_provider
                 WHERE (?::text IS NULL OR kind = ?::text) AND enabled
                 ORDER BY kind, priority, code
                """, (rs, rowNum) -> Map.of(
                "code", rs.getString("code"),
                "kind", rs.getString("kind"),
                "name", rs.getString("name"),
                "protocol", rs.getString("protocol"),
                "model", rs.getString("model"),
                "priority", rs.getInt("priority")),
                kind == null ? null : kind.name(), kind == null ? null : kind.name());
    }

    @Transactional
    public UUID enqueue(CreateJob request, String actor) {
        if (request.prompt() == null || request.prompt().isBlank()) {
            throw ApiException.badRequest("AI_PROMPT_REQUIRED", "提示词不能为空");
        }
        ProviderConfig provider = pickProvider(request.kind(), request.providerCode());
        if (!adapters.containsKey(provider.protocol())) {
            throw ApiException.conflict("AI_PROTOCOL_UNSUPPORTED",
                    "供应商协议未实现：" + provider.protocol());
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO ai_gen_job (id, kind, provider_id, product_id, sku_id, purpose, prompt,
                                        negative, params, source_asset_ids, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?)
                """, id, request.kind().name(), provider.id(), request.productId(), request.skuId(),
                request.purpose(), request.prompt().trim(), request.negative(),
                Json.write(request.params() == null ? Map.of() : request.params()),
                Json.write(mergeSources(request.sourceAssetIds(), request.images())),
                actor);
        jdbc.update("""
                INSERT INTO ai_prompt_history (id, kind, purpose, product_id, sku_id, prompt,
                                               negative, provider_code, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), request.kind().name(), request.purpose(),
                request.productId(), request.skuId(), request.prompt().trim(),
                request.negative(), provider.code(), actor);
        return id;
    }

    public Map<String, Object> job(UUID id) {
        var rows = jdbc.query("""
                SELECT j.id, j.kind, j.purpose, j.status, j.prompt, j.negative, j.error,
                       j.asset_id, j.attempts, j.created_at, p.code AS provider_code
                  FROM ai_gen_job j JOIN ai_provider p ON p.id = j.provider_id
                 WHERE j.id = ?
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getObject("id", UUID.class).toString());
            row.put("kind", rs.getString("kind"));
            row.put("purpose", rs.getString("purpose"));
            row.put("status", rs.getString("status"));
            row.put("prompt", rs.getString("prompt"));
            row.put("negative", rs.getString("negative"));
            row.put("error", rs.getString("error"));
            row.put("assetId", rs.getObject("asset_id") == null ? null
                    : rs.getObject("asset_id", UUID.class).toString());
            row.put("attempts", rs.getInt("attempts"));
            row.put("providerCode", rs.getString("provider_code"));
            row.put("createdAt", rs.getTimestamp("created_at").toInstant().toString());
            return row;
        }, id);
        if (rows.isEmpty()) {
            throw ApiException.notFound("AI_JOB_NOT_FOUND", "任务不存在：" + id);
        }
        return rows.get(0);
    }

    public List<Map<String, Object>> recentJobs(int limit) {
        return jdbc.query("""
                SELECT id, kind, purpose, status, created_at FROM ai_gen_job
                 ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> Map.of(
                "id", rs.getObject("id", UUID.class).toString(),
                "kind", rs.getString("kind"),
                "purpose", rs.getString("purpose"),
                "status", rs.getString("status"),
                "createdAt", rs.getTimestamp("created_at").toInstant().toString()),
                Math.min(Math.max(limit, 1), 100));
    }

    /** 历史提示词（同一商品/规格维度），供"直接上屏复用"。 */
    public List<Map<String, Object>> promptHistory(UUID productId, UUID skuId, int limit) {
        return jdbc.query("""
                SELECT prompt, negative, provider_code, asset_id, created_at, purpose
                  FROM ai_prompt_history
                 WHERE (?::uuid IS NULL OR product_id = ?::uuid)
                   AND (?::uuid IS NULL OR sku_id = ?::uuid)
                 ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> Map.of(
                "prompt", rs.getString("prompt"),
                "negative", rs.getString("negative") == null ? "" : rs.getString("negative"),
                "providerCode", rs.getString("provider_code") == null ? "" : rs.getString("provider_code"),
                "assetId", rs.getObject("asset_id") == null ? ""
                        : rs.getObject("asset_id", UUID.class).toString(),
                "purpose", rs.getString("purpose"),
                "createdAt", rs.getTimestamp("created_at").toInstant().toString()),
                productId, productId, skuId, skuId, Math.min(Math.max(limit, 1), 100));
    }

    /** 处理一条待执行任务；没有任务返回 false。 */
    @Transactional
    public boolean runOne() {
        var claimed = jdbc.query("""
                WITH picked AS (
                    SELECT id FROM ai_gen_job
                     WHERE status = 'PENDING'
                     ORDER BY created_at
                     FOR UPDATE SKIP LOCKED
                     LIMIT 1
                )
                UPDATE ai_gen_job j SET status = 'RUNNING', attempts = attempts + 1, updated_at = now()
                  FROM picked WHERE j.id = picked.id
                 RETURNING j.id
                """, (rs, rowNum) -> rs.getObject("id", UUID.class));
        if (claimed.isEmpty()) {
            return false;
        }
        UUID jobId = claimed.get(0);
        var jobRows = jdbc.query("""
                SELECT j.kind, j.purpose, j.prompt, j.negative, j.params::text AS params,
                       j.source_asset_ids::text AS sources, j.product_id, j.sku_id,
                       p.id AS provider_id, p.kind AS provider_kind, p.code, p.name, p.protocol,
                       p.endpoint, p.api_key, p.model, p.extra::text AS extra, p.enabled, p.priority
                  FROM ai_gen_job j JOIN ai_provider p ON p.id = j.provider_id WHERE j.id = ?
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("kind", rs.getString("kind"));
            row.put("purpose", rs.getString("purpose"));
            row.put("prompt", rs.getString("prompt"));
            row.put("negative", rs.getString("negative"));
            row.put("params", rs.getString("params"));
            row.put("sources", rs.getString("sources"));
            row.put("provider", new ProviderConfig(
                    rs.getObject("provider_id", UUID.class),
                    MediaKind.valueOf(rs.getString("provider_kind")),
                    rs.getString("code"), rs.getString("name"), rs.getString("protocol"),
                    rs.getString("endpoint"), decrypt(rs.getString("api_key")), rs.getString("model"),
                    Json.read(rs.getString("extra")), rs.getBoolean("enabled"), rs.getInt("priority")));
            return row;
        }, jobId);
        Map<String, Object> row = jobRows.get(0);
        ProviderConfig provider = (ProviderConfig) row.get("provider");
        Map<String, Object> params = row.get("params") == null ? Map.of() : toMap(row.get("params"));
        try {
            MediaGenerationAdapter adapter = adapters.get(provider.protocol());
            if (adapter == null) {
                throw new IllegalStateException("协议未实现：" + provider.protocol());
            }
            // 参考图/引导图：按 source_asset_ids 取真实 URL 传给模型，而不是只留痕
            java.util.List<Map<String, Object>> images = resolveImages(
                    String.valueOf(row.get("sources")));
            Map<String, Object> effective = new LinkedHashMap<>(params);
            if (!images.isEmpty()) {
                effective.put("images", images);
                effective.put("referenceImageUrls", images.stream()
                        .map(image -> String.valueOf(image.get("url"))).toList());
                images.stream()
                        .filter(image -> "FIRST_FRAME".equals(image.get("role")))
                        .findFirst()
                        .ifPresent(image -> effective.put("firstFrameUrl", image.get("url")));
            }
            GeneratedMedia media = adapter.generate(provider,
                    String.valueOf(row.get("prompt")),
                    row.get("negative") == null ? null : String.valueOf(row.get("negative")),
                    effective);
            UUID assetId = registerAsset(jobId, provider, (String) row.get("kind"), media);
            jdbc.update("""
                    UPDATE ai_gen_job SET status = 'SUCCEEDED', asset_id = ?, error = NULL,
                                         updated_at = now() WHERE id = ?
                    """, assetId, jobId);
            return true;
        } catch (RuntimeException error) {
            jdbc.update("""
                    UPDATE ai_gen_job SET status = 'FAILED', error = ?, updated_at = now() WHERE id = ?
                    """, abbreviate(error.getMessage()), jobId);
            throw error;
        }
    }

    private Map<String, Object> toMap(Object json) {
        try {
            return Json.mapper().readValue(String.valueOf(json),
                    new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {
                    });
        } catch (Exception error) {
            throw new IllegalStateException("任务参数解析失败", error);
        }
    }

    private UUID registerAsset(UUID jobId, ProviderConfig provider, String kind, GeneratedMedia media) {
        byte[] content = media.content();
        String url;
        if (media.hasBytes()) {
            String extension = "VIDEO".equals(kind) ? "mp4" : ("TEXT".equals(kind) ? "txt" : "png");
            String key = "ai/" + provider.code() + "/" + jobId + "." + extension;
            url = storage.put(key, content, media.contentType());
        } else {
            url = media.sourceUrl();
            if (url == null || url.isBlank()) {
                throw new IllegalStateException("生成结果既无内容也无 URL");
            }
        }
        UUID assetId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO asset (id, name, url, kind, owner_username, created_at)
                VALUES (?, ?, ?, ?, 'ai', now())
                """, assetId, "AI " + provider.code() + " " + jobId,
                url, "VIDEO".equals(kind) ? "VIDEO" : ("TEXT".equals(kind) ? "TEXT" : "IMAGE"));
        return assetId;
    }

    private ProviderConfig pickProvider(MediaKind kind, String code) {
        var rows = jdbc.query("""
                SELECT id, kind, code, name, protocol, endpoint, api_key, model, extra::text AS extra,
                       enabled, priority
                  FROM ai_provider
                 WHERE kind = ? AND enabled
                   AND (?::text IS NULL OR code = ?::text)
                 ORDER BY priority, code LIMIT 1
                """, (rs, rowNum) -> new ProviderConfig(
                rs.getObject("id", UUID.class), MediaKind.valueOf(rs.getString("kind")),
                rs.getString("code"), rs.getString("name"), rs.getString("protocol"),
                rs.getString("endpoint"), decrypt(rs.getString("api_key")), rs.getString("model"),
                Json.read(rs.getString("extra")), rs.getBoolean("enabled"), rs.getInt("priority")),
                kind.name(), code, code);
        if (rows.isEmpty()) {
            throw ApiException.conflict("AI_PROVIDER_UNAVAILABLE",
                    code == null ? ("没有可用的 " + kind + " 供应商，请先在后台配置")
                            : ("供应商不可用：" + code));
        }
        return rows.get(0);
    }

    /** 合并旧参数（纯 id）与新参数（id+角色），统一成 [{assetId,role}]。 */
    private List<Map<String, Object>> mergeSources(List<UUID> sourceAssetIds, List<ImageRef> images) {
        List<Map<String, Object>> merged = new ArrayList<>();
        if (images != null) {
            for (ImageRef image : images) {
                if (image.assetId() == null) {
                    throw ApiException.badRequest("AI_IMAGE_ASSET_REQUIRED", "参考图缺少 assetId");
                }
                String role = image.role() == null || image.role().isBlank()
                        ? "REFERENCE" : image.role().trim().toUpperCase(java.util.Locale.ROOT);
                if (!Set.of("REFERENCE", "SUBJECT", "FIRST_FRAME", "LAST_FRAME").contains(role)) {
                    throw ApiException.badRequest("AI_IMAGE_ROLE_INVALID",
                            "参考图角色只能是 REFERENCE/SUBJECT/FIRST_FRAME/LAST_FRAME，实际=" + image.role());
                }
                merged.add(Map.of("assetId", image.assetId().toString(), "role", role));
            }
        }
        if (sourceAssetIds != null) {
            for (UUID id : sourceAssetIds) {
                merged.add(Map.of("assetId", id.toString(), "role", "REFERENCE"));
            }
        }
        return merged;
    }

    /** 把 [{assetId,role}] 解析成 [{url,role}]；素材不存在直接报错，不静默丢图。 */
    private List<Map<String, Object>> resolveImages(String sourcesJson) {
        List<Map<String, Object>> images = new ArrayList<>();
        if (sourcesJson == null || sourcesJson.isBlank() || "[]".equals(sourcesJson)) {
            return images;
        }
        for (com.fasterxml.jackson.databind.JsonNode node : Json.read(sourcesJson)) {
            String id = node.path("assetId").asText("");
            String role = node.path("role").asText("REFERENCE");
            if (id.isBlank()) {
                continue;
            }
            var rows = jdbc.query("SELECT url FROM asset WHERE id = ?::uuid",
                    (rs, rowNum) -> rs.getString("url"), id);
            if (rows.isEmpty()) {
                throw new IllegalStateException("参考图不存在：" + id);
            }
            Map<String, Object> image = new LinkedHashMap<>();
            image.put("assetId", id);
            image.put("url", rows.get(0));
            image.put("role", role);
            images.add(image);
        }
        return images;
    }

    private String decrypt(String stored) {
        if (stored == null || stored.isBlank()) {
            return "";
        }
        return cipher.decrypt(stored);
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "未知错误";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    /** 提示词优化（可选）：走 openai-chat 供应商，未配置则明确不可用。 */
    public String optimizePrompt(String prompt, String providerCode) {
        if (prompt == null || prompt.isBlank()) {
            throw ApiException.badRequest("AI_PROMPT_REQUIRED", "提示词不能为空");
        }
        ProviderConfig provider = pickProvider(MediaKind.TEXT, providerCode);
        MediaGenerationAdapter adapter = adapters.get(provider.protocol());
        if (adapter == null) {
            throw ApiException.conflict("AI_PROTOCOL_UNSUPPORTED", "协议未实现：" + provider.protocol());
        }
        var media = adapter.generate(provider,
                "把下面的电商素材提示词改写得更具体（主体/构图/光线/背景/风格），只输出改写后的提示词：\n" + prompt,
                null, Map.of("system", "你是资深电商视觉提示词工程师，输出中文，不超过 200 字。"));
        return new String(media.content() == null ? new byte[0] : media.content(), StandardCharsets.UTF_8);
    }

    /** 同步取 embedding 文本（向量以 JSON 数组返回），供语义检索使用。 */
    public String embedding(String text, String providerCode) {
        // 必须按协议挑：TEXT 类型里还有 chat 供应商，按优先级盲选会选错
        ProviderConfig provider = pickProviderByProtocol("openai-embedding", providerCode);
        MediaGenerationAdapter adapter = adapters.get(provider.protocol());
        if (adapter == null) {
            throw ApiException.conflict("AI_EMBEDDING_UNAVAILABLE",
                    "embedding 适配器未注册：" + provider.protocol());
        }
        GeneratedMedia media = adapter.generate(provider, text, null, Map.of());
        if (!media.hasBytes()) {
            throw new IllegalStateException("embedding 未返回内容");
        }
        return new String(media.content(), StandardCharsets.UTF_8);
    }

    private ProviderConfig pickProviderByProtocol(String protocol, String code) {
        var rows = jdbc.query("""
                SELECT id, kind, code, name, protocol, endpoint, api_key, model, extra::text AS extra,
                       enabled, priority
                  FROM ai_provider
                 WHERE protocol = ? AND enabled
                   AND (?::text IS NULL OR code = ?::text)
                 ORDER BY priority, code LIMIT 1
                """, (rs, rowNum) -> new ProviderConfig(
                rs.getObject("id", UUID.class), MediaKind.valueOf(rs.getString("kind")),
                rs.getString("code"), rs.getString("name"), rs.getString("protocol"),
                rs.getString("endpoint"), decrypt(rs.getString("api_key")), rs.getString("model"),
                Json.read(rs.getString("extra")), rs.getBoolean("enabled"), rs.getInt("priority")),
                protocol, code, code);
        if (rows.isEmpty()) {
            throw ApiException.conflict("AI_EMBEDDING_UNAVAILABLE",
                    "没有可用的 embedding 供应商（需要 protocol=openai-embedding"
                            + (code == null ? "" : "，指定 code=" + code) + "）");
        }
        return rows.get(0);
    }
}
