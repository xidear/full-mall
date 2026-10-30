package com.fullmall.asset.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 素材的语言变体（asset × locale → URL）。
 *
 * <p>为什么不是"复制一份素材"：同一张主图的中英文版本是同一个业务素材，
 * 复制成两条会让引用关系、统计、清理都变复杂；这里只存"某语言用哪个 URL"。
 *
 * <p>渲染时替换而不是写死：详情 HTML 里存的是默认语言图片地址，
 * 输出前按当前 locale 把 src 换成本地化版本，编辑内容与语言解耦。
 */
@Service
public class AssetLocaleVariantService {

    private final JdbcTemplate jdbc;

    public AssetLocaleVariantService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Variant(UUID assetId, String locale, String url) {
    }

    public List<Variant> list(UUID assetId) {
        return jdbc.query("""
                SELECT asset_id, locale, url FROM asset_locale_variant
                 WHERE asset_id = ? ORDER BY locale
                """, (rs, rowNum) -> new Variant(rs.getObject("asset_id", UUID.class),
                rs.getString("locale"), rs.getString("url")), assetId);
    }

    /** 新增/更新某语言的变体；locale 必须是已启用的语言，避免存了用不上的组合。 */
    public Variant upsert(UUID assetId, String locale, String url) {
        if (locale == null || locale.isBlank()) {
            throw ApiException.badRequest("LOCALE_REQUIRED", "语言不能为空");
        }
        if (url == null || url.isBlank()) {
            throw ApiException.badRequest("VARIANT_URL_REQUIRED", "变体图片地址不能为空");
        }
        Integer assets = jdbc.queryForObject("SELECT COUNT(*) FROM asset WHERE id = ? AND deleted_at IS NULL",
                Integer.class, assetId);
        if (assets == null || assets == 0) {
            throw ApiException.notFound("ASSET_NOT_FOUND", "素材不存在：" + assetId);
        }
        jdbc.update("""
                INSERT INTO asset_locale_variant (asset_id, locale, url)
                VALUES (?, ?, ?)
                ON CONFLICT (asset_id, locale) DO UPDATE SET url = EXCLUDED.url
                """, assetId, locale, url.trim());
        return new Variant(assetId, locale, url.trim());
    }

    public void delete(UUID assetId, String locale) {
        int deleted = jdbc.update("DELETE FROM asset_locale_variant WHERE asset_id = ? AND locale = ?",
                assetId, locale);
        if (deleted == 0) {
            throw ApiException.notFound("VARIANT_NOT_FOUND",
                    "该素材没有语言变体：" + assetId + " / " + locale);
        }
    }

    /** 默认语言 URL → 该语言 URL 的映射（渲染时用；没有变体的图片保持原样）。 */
    public Map<String, String> urlMap(String locale) {
        if (locale == null || locale.isBlank()) {
            return Map.of();
        }
        Map<String, String> map = new LinkedHashMap<>();
        jdbc.query("""
                SELECT a.url AS default_url, v.url AS locale_url
                  FROM asset_locale_variant v
                  JOIN asset a ON a.id = v.asset_id AND a.deleted_at IS NULL
                 WHERE v.locale = ?
                """, rs -> {
            map.put(rs.getString("default_url"), rs.getString("locale_url"));
        }, locale);
        return map;
    }
}
