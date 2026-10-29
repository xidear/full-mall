package com.fullmall.catalog.app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 富文本详情翻译：**只翻译文本节点，结构原样保留**。
 *
 * <p>为什么不用"整段 HTML 丢给模型"：
 * <ul>
 *   <li>模型可能改动标签/属性（style、class、内联 video、base64），前端渲染不可控；</li>
 *   <li>无法逐段审核：运营只想改一段时，整篇重翻会覆盖人工修改；</li>
 *   <li>计费按整篇：改一个字就要重翻全文。</li>
 * </ul>
 *
 * <p>做法：
 * <ol>
 *   <li>jsoup 解析 → 逐"块级元素"（p / h1-h6 / li / td / figcaption / blockquote）取值；</li>
 *   <li>段内保留行内标签（strong / em / a / br）：只替换文本节点，标签与顺序不变；</li>
 *   <li>跳过 `code`、`pre`、`[data-no-translate]`（品牌名、SKU 编码、法务条款原文）；</li>
 *   <li>每段文本走 {@link TranslationService}：先查翻译记忆，命中不重复计费（**改一段只翻一段**）；</li>
 *   <li>图片/视频天然与语言无关，原样保留；需要本地化图时用图片上的 data-locale-src 覆盖（后续）。</li>
 * </ol>
 */
@Service
public class RichTextTranslationService {

    private static final List<String> BLOCK_TAGS = List.of(
            "p", "h1", "h2", "h3", "h4", "h5", "h6", "li", "dt", "dd",
            "blockquote", "figcaption", "td", "th");
    private static final List<String> SKIP_TAGS = List.of("code", "pre", "script", "style");

    private final TranslationService translator;

    public RichTextTranslationService(TranslationService translator) {
        this.translator = translator;
    }

    public record Segment(String source, String translated, String provider) {
    }

    public record Result(String html, int segments, int cached, List<Segment> details) {
    }

    public Result translateHtml(String html, String locale, int glossaryVersion) {
        if (html == null || html.isBlank()) {
            return new Result("", 0, 0, List.of());
        }
        Document document = Jsoup.parseBodyFragment(html);
        document.outputSettings().prettyPrint(false);

        List<Segment> segments = new ArrayList<>();
        int cached = 0;
        for (Element block : document.body().select(String.join(", ", BLOCK_TAGS))) {
            if (isSkipped(block)) {
                continue;
            }
            for (TextNode textNode : textNodesOf(block)) {
                String source = textNode.getWholeText();
                if (source == null || source.isBlank()) {
                    continue;
                }
                // 保留原有首尾空白，避免拼接后中文/英文挤在一起
                String leading = source.substring(0, source.length() - source.stripLeading().length());
                String trailing = source.substring(source.stripTrailing().length());
                TranslationService.Translation translation =
                        translator.translate(source.trim(), locale, glossaryVersion);
                textNode.text(leading + translation.text() + trailing);
                if ("memory".equals(translation.source())) {
                    cached++;
                }
                segments.add(new Segment(source.trim(), translation.text(), translation.source()));
            }
        }
        return new Result(document.body().html(), segments.size(), cached, segments);
    }

    /** 块内所有"需要翻译"的文本节点（跳过 code/pre/data-no-translate 与其子树）。 */
    private List<TextNode> textNodesOf(Element block) {
        List<TextNode> result = new ArrayList<>();
        collect(block, result);
        return result;
    }

    private void collect(Node node, List<TextNode> out) {
        for (Node child : node.childNodes()) {
            if (child instanceof TextNode textNode) {
                out.add(textNode);
                continue;
            }
            if (child instanceof Element element) {
                if (isSkipped(element)) {
                    continue;
                }
                collect(element, out);
            }
        }
    }

    private boolean isSkipped(Element element) {
        if (SKIP_TAGS.contains(element.normalName())) {
            return true;
        }
        return element.hasAttr("data-no-translate")
                || element.closest("[data-no-translate]") != null;
    }
}
