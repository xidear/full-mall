package com.fullmall.asset.app;

import com.fullmall.ai.knowledge.SemanticIndexService;
import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 素材检索：服务端分页 + 分类/标签 + 关键词 + 语义（向量）检索。
 *
 * <p>为什么不再"加载更多 + 前端过滤"：
 * 素材库会到几千条，前端一次拉 60 条再过滤等于"只能搜到已加载的那部分"。
 *
 * <p>为什么要有标签/描述：
 * 图片文件名（m.png / picker.png / IMG_1234.jpg）几乎不携带语义，
 * 只按标题搜索等于"搜不到东西"。标签与描述可以被人工维护，也可以由视觉模型生成，
 * 再用于向量检索（"白底杯子""厨房场景"这类自然语言查询）。
 */
@Service
public class AssetSearchService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(AssetSearchService.class);

    private final JdbcTemplate jdbc;
    private final SemanticIndexService semantic;
    private final com.fullmall.ai.media.AiMediaService media;
    /** 本站素材的本地根目录：视觉模型需要图片字节，用它把 /assets/** 转成 data URL。 */
    private final String localDirectory;

    public AssetSearchService(JdbcTemplate jdbc, SemanticIndexService semantic,
                              com.fullmall.ai.media.AiMediaService media,
                              @org.springframework.beans.factory.annotation.Value("${app.asset.local.directory:./data/assets}") String localDirectory) {
        this.jdbc = jdbc;
        this.semantic = semantic;
        this.media = media;
        this.localDirectory = localDirectory;
    }

    public record AssetRow(UUID id, String name, String url, String kind, String purpose,
                           String caption, List<String> tags, java.time.Instant createdAt) {
    }

    public record AssetPage(List<AssetRow> items, long total, int page, int size) {
    }

    public AssetPage search(String keyword, String tag, String purpose, String kind, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String tagFilter = tag == null || tag.isBlank() ? null : tag.trim();
        String purposeFilter = purpose == null || purpose.isBlank() ? null : purpose.trim();
        String kindFilter = kind == null || kind.isBlank() ? null : kind.trim();
        String like = kw == null ? null : "%" + kw.toLowerCase() + "%";

        String where = """
                FROM asset
                 WHERE deleted_at IS NULL
                   AND (?::text IS NULL OR purpose = ?::text)
                   AND (?::text IS NULL OR kind = ?::text)
                   AND (?::text[] IS NULL OR tags && ?::text[])
                   AND (?::text IS NULL OR lower(name) LIKE ?::text
                        OR lower(coalesce(caption, '')) LIKE ?::text
                        OR lower(array_to_string(tags, ' ')) LIKE ?::text)
                """;
        Object[] args = {purposeFilter, purposeFilter, kindFilter, kindFilter,
                tagFilter == null ? null : new String[]{tagFilter},
                tagFilter == null ? null : new String[]{tagFilter},
                like, like, like, like};

        Long total = jdbc.queryForObject("SELECT COUNT(*) " + where, Long.class, args);
        // List.of(...) 不接受 null（筛选条件为空时就是 null），必须用 Arrays.asList
        List<Object> pageArgs = new ArrayList<>(java.util.Arrays.asList(args));
        pageArgs.add(safeSize);
        pageArgs.add(safeOffset(safePage, safeSize));
        List<AssetRow> items = jdbc.query("""
                SELECT id, name, url, kind, purpose, caption, tags, created_at
                """ + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapRow(rs), pageArgs.toArray());
        return new AssetPage(items, total == null ? 0 : total, safePage, safeSize);
    }

    /** 标签及其使用次数（筛选栏用）。 */
    public List<Map<String, Object>> tags() {
        return jdbc.query("""
                SELECT tag, COUNT(*) AS count
                  FROM (SELECT unnest(tags) AS tag FROM asset WHERE deleted_at IS NULL) t
                 GROUP BY tag ORDER BY count DESC, tag
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tag", rs.getString("tag"));
            row.put("count", rs.getInt("count"));
            return row;
        });
    }

    /**
     * 维护标签与描述，并重建向量索引（语义检索的内容来源）。
     *
     * <p>**不放在一个事务里**：embedding 是一次 HTTP 调用（最长 60s），
     * 拿事务会把数据库连接一直占着——30 条素材重建就能把连接池（默认 20）耗尽，
     * 其它接口随之 500（实测 HikariPool Connection is not available）。
     */
    public AssetRow updateMeta(UUID assetId, List<String> tags, String caption) {
        List<String> cleaned = new ArrayList<>();
        if (tags != null) {
            for (String tag : tags) {
                String value = tag == null ? "" : tag.trim();
                if (!value.isEmpty() && !cleaned.contains(value)) {
                    cleaned.add(value);
                }
            }
        }
        int updated = jdbc.update("UPDATE asset SET tags = ?, caption = ?, updated_at = now() WHERE id = ?",
                cleaned.toArray(new String[0]), caption == null || caption.isBlank() ? null : caption.trim(),
                assetId);
        if (updated != 1) {
            throw ApiException.notFound("ASSET_NOT_FOUND", "素材不存在：" + assetId);
        }
        reindex(assetId);
        return find(assetId);
    }

    /**
     * AI 识别图片内容：调用视觉模型生成描述与标签，写入素材并重建索引。
     *
     * <p>图片要先变成「模型侧可访问」的地址：本站存储的 /assets/** 读本地文件转成 data URL，
     * 公网 http(s) 地址直接透传。模型输出不是 JSON 时按原文当描述（parsed=false 明确告诉调用方）。
     */
    public DescribeResult describeByVision(UUID assetId, String providerCode) {
        AssetRow asset = find(assetId);
        if (!"IMAGE".equalsIgnoreCase(asset.kind())) {
            throw ApiException.badRequest("ASSET_NOT_IMAGE", "只有图片素材可以做内容识别：" + asset.kind());
        }
        String raw = media.describe(imageDataUrl(asset.url()), providerCode);
        String caption = raw == null ? "" : raw.trim().substring(0, Math.min(raw.trim().length(), 500));
        List<String> tags = List.of();
        boolean parsed = false;
        try {
            var node = com.fullmall.platform.json.Json.read(raw);
            String parsedCaption = node.path("caption").asText("");
            var tagNodes = node.path("tags");
            if (!parsedCaption.isBlank()) {
                caption = parsedCaption.trim();
                parsed = true;
            }
            if (tagNodes.isArray()) {
                List<String> parsedTags = new ArrayList<>();
                tagNodes.forEach(item -> {
                    String value = item.asText("");
                    if (!value.isBlank() && !parsedTags.contains(value.trim())) {
                        parsedTags.add(value.trim());
                    }
                });
                tags = parsedTags;
            }
        } catch (RuntimeException parseError) {
            // 小模型（如 moondream）常不按 JSON 输出：整段当描述，标签用本地规则从描述里抽，
            // 至少保证"理解型搜索"能命中（parsed=false 让调用方知道不是结构化结果）。
            caption = raw.trim();
            tags = AssetTextKeywords.of(caption);
        }
        AssetRow updated = updateMeta(assetId, tags, caption);
        return new DescribeResult(updated, raw, parsed);
    }

    public record DescribeResult(AssetRow asset, String raw, boolean parsed) {
    }

    /** 把本站存储的相对地址转成 data URL；公网地址原样返回。 */
    private String imageDataUrl(String url) {
        if (url == null || url.isBlank()) {
            throw ApiException.badRequest("ASSET_URL_REQUIRED", "素材缺少图片地址");
        }
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:")) {
            return url;
        }
        String relative = url.startsWith("/assets/") ? url.substring("/assets/".length())
                : url.replaceFirst("^/", "");
        // /assets/** 的剩余部分本身就带 assets/ 前缀（见 StaticAssetConfig），
        // 所以相对路径直接拼在存储根目录下，不能再补一层 assets/
        java.nio.file.Path path = java.nio.file.Path.of(localDirectory)
                .resolve(relative).normalize();
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(path);
            String contentType = relative.endsWith(".png") ? "image/png"
                    : relative.endsWith(".webp") ? "image/webp"
                    : relative.endsWith(".gif") ? "image/gif" : "image/jpeg";
            return "data:" + contentType + ";base64,"
                    + java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (java.io.IOException error) {
            throw new IllegalStateException(
                    "读取素材文件失败（视觉模型需要图片字节）：" + path, error);
        }
    }

    /** 语义检索：把自然语言查询向量化，与素材描述/标签的向量比较。 */
    public List<Map<String, Object>> semanticSearch(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw ApiException.badRequest("ASSET_SEARCH_QUERY_REQUIRED", "查询内容不能为空");
        }
        List<Map<String, Object>> hits = semantic.search(query.trim(), "ASSET", limit, null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> hit : hits) {
            UUID id = UUID.fromString(String.valueOf(hit.get("sourceId")));
            AssetRow asset = find(id);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("asset", asset);
            row.put("content", hit.get("content"));
            row.put("score", hit.get("score"));
            result.add(row);
        }
        return result;
    }

    /** 重建全部素材索引（首次启用语义检索、或批量补标签后用）。同样不占事务。 */
    public int reindexAll() {
        List<UUID> ids = jdbc.queryForList(
                "SELECT id FROM asset WHERE deleted_at IS NULL", UUID.class);
        int count = 0;
        for (UUID id : ids) {
            try {
                reindex(id);
            } catch (ApiException unavailable) {
                throw unavailable;
            } catch (RuntimeException error) {
                throw new IllegalStateException("素材索引失败：" + id, error);
            }
            count++;
        }
        return count;
    }

    /**
     * 重建语义索引。
     *
     * <p>没配 embedding 供应商时**跳过并记录**，不抛异常：索引是检索增强，
     * 不该让"生成图片/编辑素材"这种主流程失败（此前会连累 AI 生成直接报错）。
     * 配了供应商但调用失败仍然抛错——那是真故障，不能当成"没配"吞掉。
     */
    public void reindex(UUID assetId) {
        if (!semantic.available(null)) {
            log.info("语义索引未启用（semantic.enabled={} 或未配 openai-embedding 供应商），跳过重建：asset={}",
                    semantic.enabled(), assetId);
            return;
        }
        AssetRow asset = find(assetId);
        String content = contentOf(asset);
        semantic.index("ASSET", assetId.toString(), content, null);
        jdbc.update("UPDATE asset SET indexed_at = now() WHERE id = ?", assetId);
    }

    private String contentOf(AssetRow asset) {
        StringBuilder text = new StringBuilder(asset.name() == null ? "" : asset.name());
        if (asset.caption() != null && !asset.caption().isBlank()) {
            text.append("。").append(asset.caption());
        }
        if (asset.tags() != null && !asset.tags().isEmpty()) {
            text.append("。标签：").append(String.join("、", asset.tags()));
        }
        return text.toString();
    }

    public AssetRow find(UUID assetId) {
        List<AssetRow> rows = jdbc.query("""
                SELECT id, name, url, kind, purpose, caption, tags, created_at
                  FROM asset WHERE id = ? AND deleted_at IS NULL
                """, (rs, rowNum) -> mapRow(rs), assetId);
        return rows.stream().findFirst().orElseThrow(() ->
                ApiException.notFound("ASSET_NOT_FOUND", "素材不存在：" + assetId));
    }

    private AssetRow mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.sql.Array sqlArray = rs.getArray("tags");
        String[] tags = sqlArray == null ? new String[0] : (String[]) sqlArray.getArray();
        return new AssetRow(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("url"),
                rs.getString("kind"),
                rs.getString("purpose"),
                rs.getString("caption"),
                tags == null ? List.of() : List.of(tags),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toInstant());
    }

    private int safeOffset(int page, int size) {
        return page * size;
    }
}
