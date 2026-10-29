package com.fullmall.platform.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 富文本白名单清洗（商品详情等运营可编辑 HTML）。
 *
 * <p>前端编辑器（TipTap）只限制可插入内容，**服务端才是权威**：请求可绕过编辑器，
 * 直接 PUT 一段带 script/onerror 的 HTML 就会打进商城页面。
 *
 * <p>为什么媒体地址要单独校验：本站图片/视频是相对路径（/images/…、/assets/…），
 * jsoup 的协议白名单只认带协议的绝对地址，配上就会把相对路径整段删掉（详情里的图全没）。
 * 因此媒体 src 不用 jsoup 协议校验，改为清洗后逐条检查 scheme。
 */
@Component
public class RichTextSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("figure", "figcaption", "video", "source")
            .addAttributes("video", "src", "controls", "poster", "width", "height", "playsinline")
            .addAttributes("source", "src", "type")
            .addAttributes("img", "src", "alt", "width", "height")
            .addAttributes("a", "href", "title", "target", "rel")
            // Safelist.relaxed() 自带给 img/src 的协议白名单（只认 http/https），
            // 会把 /images/... 这类站内相对路径整段删掉；这里移除，改由下面的 isSafeMediaUrl 校验。
            .removeProtocols("img", "src", "http", "https");

    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        // prettyPrint(false)：默认的换行/缩进会变成文本节点，导致 figcaption 之类被插进空白
        Document document = Jsoup.parseBodyFragment(
                Jsoup.clean(html, "", SAFELIST, new Document.OutputSettings().prettyPrint(false)));
        document.outputSettings().prettyPrint(false);
        document.select("img, video, source").forEach(element -> {
            if (!isSafeMediaUrl(element.attr("src"))) {
                element.remove();
            }
        });
        document.select("a[href]").forEach(element -> {
            if (!isSafeLink(element.attr("href"))) {
                element.removeAttr("href");
            }
        });
        return document.body().html();
    }

    /** 站内相对路径、http(s) 与内联图片/视频允许；javascript:/vbscript: 等一律拒绝。 */
    private boolean isSafeMediaUrl(String src) {
        if (src == null || src.isBlank()) {
            return false;
        }
        if (src.startsWith("/")) {
            return true;
        }
        String value = src.toLowerCase(Locale.ROOT);
        return value.startsWith("http://") || value.startsWith("https://")
                || value.startsWith("data:image/") || value.startsWith("data:video/");
    }

    private boolean isSafeLink(String href) {
        if (href == null || href.isBlank() || href.startsWith("/")) {
            return href != null && !href.isBlank();
        }
        String value = href.toLowerCase(Locale.ROOT);
        return value.startsWith("http://") || value.startsWith("https://")
                || value.startsWith("mailto:");
    }
}
