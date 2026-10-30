package com.fullmall.platform.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /** 唯一放行的 CSS 声明；其余 style 一律丢弃（见 normalizeTextAlign）。 */
    private static final Pattern TEXT_ALIGN = Pattern.compile("text-align\\s*:\\s*(left|center|right)",
            Pattern.CASE_INSENSITIVE);

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("figure", "figcaption", "video", "source")
            .addAttributes("video", "src", "controls", "poster", "width", "height", "playsinline", "data-asset-id")
            .addAttributes("source", "src", "type")
            .addAttributes("img", "src", "alt", "width", "height", "data-asset-id")
            // style 只给段落与标题开口子，仅供 text-align 使用；清洗后由 normalizeTextAlign 删掉其它声明。
            // 不加这一步，编辑器的"居中/左对齐"会在保存时静默消失（运营只会觉得"点了没用"）。
            .addAttributes("p", "style")
            .addAttributes("h1", "style")
            .addAttributes("h2", "style")
            .addAttributes("h3", "style")
            .addAttributes("h4", "style")
            .addAttributes("h5", "style")
            .addAttributes("h6", "style")
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
        document.select("[style]").forEach(this::normalizeTextAlign);
        return document.body().html();
    }

    /**
     * style 只保留一条 text-align。
     *
     * <p>白名单放行 style 等于放行任意 CSS：position:fixed 能盖住购买按钮，background:url() 能把访客
     * 引去外站探针。这里把它收窄成"对齐"这一种表达，其它声明整条删掉——宁可丢样式，也不让 style 变成注入面。
     */
    private void normalizeTextAlign(Element element) {
        Matcher matcher = TEXT_ALIGN.matcher(element.attr("style"));
        if (matcher.find()) {
            element.attr("style", "text-align:" + matcher.group(1).toLowerCase(Locale.ROOT));
        } else {
            element.removeAttr("style");
        }
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
