package com.fullmall.catalog.app;

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
 * 商品/规格素材编排：多图、主图、详情图、视频、规格专用图。
 *
 * <p>素材本体在 `asset`（含 AI 生成结果），这里只维护"商品 ↔ 素材 + 角色 + 排序"。
 * 同一商品只允许一个主图（数据库有部分唯一索引兜底）。
 */
@Service
public class ProductMediaService {

    public static final Set<String> ROLES = Set.of("MAIN", "GALLERY", "DETAIL", "VIDEO", "SPEC");

    private final JdbcTemplate jdbc;

    public ProductMediaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(UUID productId) {
        return jdbc.query("""
                SELECT m.id, m.sku_id, m.asset_id, m.role, m.sort_order, m.source,
                       a.url, a.name, a.kind
                  FROM product_media m JOIN asset a ON a.id = m.asset_id
                 WHERE m.product_id = ?
                 ORDER BY m.role, m.sort_order, m.created_at
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getObject("id", UUID.class).toString());
            row.put("skuId", rs.getObject("sku_id") == null ? null
                    : rs.getObject("sku_id", UUID.class).toString());
            row.put("assetId", rs.getObject("asset_id", UUID.class).toString());
            row.put("role", rs.getString("role"));
            row.put("sortOrder", rs.getInt("sort_order"));
            row.put("source", rs.getString("source"));
            row.put("url", rs.getString("url"));
            row.put("name", rs.getString("name"));
            row.put("kind", rs.getString("kind"));
            return row;
        }, productId);
    }

    @Transactional
    public UUID attach(UUID productId, UUID skuId, UUID assetId, String role, int sortOrder,
                       String source, String actor) {
        String normalized = role == null ? "" : role.trim().toUpperCase(java.util.Locale.ROOT);
        if (!ROLES.contains(normalized)) {
            throw ApiException.badRequest("MEDIA_ROLE_INVALID",
                    "角色只能是 MAIN/GALLERY/DETAIL/VIDEO/SPEC，实际=" + role);
        }
        if ("SPEC".equals(normalized) && skuId == null) {
            throw ApiException.badRequest("MEDIA_SPEC_REQUIRES_SKU", "规格专用图必须指定 skuId");
        }
        if ("MAIN".equals(normalized) && skuId != null) {
            throw ApiException.badRequest("MEDIA_MAIN_IS_PRODUCT_LEVEL",
                    "主图是商品级，不要绑到具体规格；规格图请用 SPEC");
        }
        String src = source == null || source.isBlank() ? "UPLOAD"
                : source.trim().toUpperCase(java.util.Locale.ROOT);
        if (!Set.of("UPLOAD", "AI").contains(src)) {
            throw ApiException.badRequest("MEDIA_SOURCE_INVALID", "来源只能是 UPLOAD 或 AI");
        }
        Integer assetExists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM asset WHERE id = ?", Integer.class, assetId);
        if (assetExists == null || assetExists == 0) {
            throw ApiException.notFound("ASSET_NOT_FOUND", "素材不存在：" + assetId);
        }
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO product_media (id, product_id, sku_id, asset_id, role, sort_order,
                                               source, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, productId, skuId, assetId, normalized, sortOrder, src, actor);
        } catch (org.springframework.dao.DuplicateKeyException error) {
            // 唯一索引：同商品只能一个主图 / 同一素材重复挂同一角色
            throw ApiException.conflict("MEDIA_DUPLICATE",
                    "该商品已存在同类素材（主图只能一个；同一素材不能重复挂同一角色）");
        }
        return id;
    }

    @Transactional
    public void detach(UUID productId, UUID mediaId) {
        int removed = jdbc.update("DELETE FROM product_media WHERE id = ? AND product_id = ?",
                mediaId, productId);
        if (removed != 1) {
            throw ApiException.notFound("MEDIA_NOT_FOUND", "素材关系不存在：" + mediaId);
        }
    }
}
