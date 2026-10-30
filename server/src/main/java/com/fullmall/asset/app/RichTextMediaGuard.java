package com.fullmall.asset.app;

import com.fullmall.platform.error.ApiException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 富文本详情的"媒体必须来自素材库"闸门。
 *
 * <p>存在的理由：清洗器只保证"没有 XSS"，不保证"素材在库里"。绕过编辑器直接 PUT 一段 HTML，
 * 就能把外站图片、base64 内联大图写进详情——素材脱离素材库（不可检索、不可复用、无法统计），
 * 外站图还会因防盗链裂图，并把访客 IP 送给第三方。这里在写入前逐条核对。
 *
 * <p>放行规则（按顺序）：
 * <ol>
 *   <li>站内内置演示图 {@code /images/…}：随应用打包，不是运营上传物，本身不可管理；</li>
 *   <li>{@code src} 能在 {@code asset.url} 或 {@code asset_locale_variant.url} 命中；</li>
 *   <li>元素带 {@code data-asset-id} 且该素材存在——素材换 CDN 或用本地化变体地址时，
 *       URL 与库里的记录可能不一致，靠 id 兜住，不至于误判。</li>
 * </ol>
 * 其余（外站 http(s)、base64、相对路径但库里没有）一律拒绝，报 RICH_TEXT_MEDIA_NOT_IN_LIBRARY。
 */
@Component
public class RichTextMediaGuard {

    /** 随应用打包的演示图目录，与 StaticAssetConfig 的 /images/** 一致。 */
    private static final String BUNDLED_PREFIX = "/images/";

    private final JdbcTemplate jdbc;

    public RichTextMediaGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @throws ApiException 有媒体不在素材库时：错误码 RICH_TEXT_MEDIA_NOT_IN_LIBRARY，
     *                      消息里列出前几个地址，管理员据此定位是哪张图（§10.1 管理端报错要能排障）
     */
    public void requireLibraryMedia(String html) {
        if (html == null || html.isBlank()) {
            return;
        }
        Document document = Jsoup.parseBodyFragment(html);
        List<Element> media = mediaElements(document);
        if (media.isEmpty()) {
            return;
        }
        Set<String> urls = new LinkedHashSet<>();
        Set<String> ids = new LinkedHashSet<>();
        for (Element element : media) {
            String src = element.attr("src").trim();
            String id = element.attr("data-asset-id").trim();
            if (!src.isEmpty() && !isBundled(src)) {
                urls.add(src);
            }
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        Set<String> knownUrls = existingUrls(urls);
        Set<String> knownIds = existingIds(ids);

        List<String> missing = new ArrayList<>();
        for (Element element : media) {
            String src = element.attr("src").trim();
            String id = element.attr("data-asset-id").trim();
            boolean allowed = isBundled(src)
                    || knownUrls.contains(src)
                    || (!id.isEmpty() && knownIds.contains(id));
            if (!allowed) {
                missing.add(src.isEmpty() ? "(无 src，asset-id=" + id + ")" : src);
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        throw ApiException.badRequest("RICH_TEXT_MEDIA_NOT_IN_LIBRARY",
                "富文本里有 " + missing.size() + " 个媒体地址不在素材库：" + preview(missing)
                        + "。请先在素材库上传（编辑器里有上传按钮），再从素材库插入；"
                        + "外站图片与 base64 内联图片不允许写进详情——素材会因此脱离素材库、无法检索复用。");
    }

    /** 纯函数版本，便于不起数据库就单测判定规则。 */
    static List<String> missingLibraryMedia(String html, Predicate<String> urlKnown, Predicate<String> idKnown) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        List<String> missing = new ArrayList<>();
        for (Element element : mediaElements(Jsoup.parseBodyFragment(html))) {
            String src = element.attr("src").trim();
            String id = element.attr("data-asset-id").trim();
            boolean allowed = isBundled(src) || urlKnown.test(src) || (!id.isEmpty() && idKnown.test(id));
            if (!allowed) {
                missing.add(src.isEmpty() ? "(无 src)" : src);
            }
        }
        return missing;
    }

    private static List<Element> mediaElements(Document document) {
        List<Element> media = new ArrayList<>();
        document.select("img[src], video[src], source[src]").forEach(media::add);
        // 没有 src、只写了 data-asset-id 的元素也要校验：否则可以靠删 src 绕过
        document.select("img[data-asset-id], video[data-asset-id]").forEach(element -> {
            if (!media.contains(element)) {
                media.add(element);
            }
        });
        return media;
    }

    private static boolean isBundled(String src) {
        return src.startsWith(BUNDLED_PREFIX);
    }

    private static String preview(List<String> missing) {
        int limit = Math.min(missing.size(), 3);
        String joined = String.join("、", missing.subList(0, limit));
        return missing.size() > limit ? joined + " 等" : joined;
    }

    private Set<String> existingUrls(Set<String> urls) {
        if (urls.isEmpty()) {
            return Set.of();
        }
        Set<String> known = new LinkedHashSet<>(queryIn(
                "SELECT url FROM asset WHERE deleted_at IS NULL AND url IN (%s)", urls));
        known.addAll(queryIn("SELECT url FROM asset_locale_variant WHERE url IN (%s)", urls));
        return known;
    }

    private Set<String> existingIds(Set<String> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        Set<String> valid = new LinkedHashSet<>();
        for (String id : ids) {
            try {
                valid.add(UUID.fromString(id).toString());
            } catch (IllegalArgumentException notAUuid) {
                // 不是 UUID 的 id 一定不在库里；保留原值让上层按"不命中"处理即可，不必打断校验
                valid.add(id);
            }
        }
        Set<String> known = new LinkedHashSet<>(queryIn(
                "SELECT id::text FROM asset WHERE deleted_at IS NULL AND id::text IN (%s)", valid));
        return known;
    }

    /** 占位符由调用方拼，值始终走绑定参数：不把外部字符串拼进 SQL。 */
    private List<String> queryIn(String template, Set<String> values) {
        String placeholders = String.join(",", java.util.Collections.nCopies(values.size(), "?"));
        return jdbc.queryForList(String.format(template, placeholders), String.class, values.toArray());
    }
}
