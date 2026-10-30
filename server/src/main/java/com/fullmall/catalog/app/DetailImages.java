package com.fullmall.catalog.app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Map;

/**
 * 详情 HTML 里的图片按语言替换（纯函数，便于单测）。
 *
 * <p>只改 {@code <img>} 的 src，不动其它标签/属性；没有映射的图片保持原样
 * （历史上清洗器曾把不认识的相对路径整段删掉，这里绝不做删除）。
 */
final class DetailImages {

    private DetailImages() {
    }

    static String localize(String html, Map<String, String> mapping) {
        if (html == null || html.isBlank() || mapping == null || mapping.isEmpty()
                || !html.contains("<img")) {
            return html;
        }
        Document document = Jsoup.parseBodyFragment(html);
        document.outputSettings().prettyPrint(false);
        document.select("img[src]").forEach(image -> {
            String localized = mapping.get(image.attr("src"));
            if (localized != null) {
                image.attr("src", localized);
            }
        });
        return document.body().html();
    }
}
