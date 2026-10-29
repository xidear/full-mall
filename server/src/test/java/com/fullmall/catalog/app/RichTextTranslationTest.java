package com.fullmall.catalog.app;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 富文本详情翻译：只替换文本节点，结构与标签保持不变。
 *
 * <p>模型不碰 HTML —— 换来的好处是：结构零风险、可逐段审核、改一段只翻一段（翻译记忆命中不计费）。
 */
class RichTextTranslationTest {

    private final TranslationService translator = mock(TranslationService.class);
    private final RichTextTranslationService service = new RichTextTranslationService(translator);

    private void echoTranslation(int glossaryVersion) {
        when(translator.translate(anyString(), eq("en"), eq(glossaryVersion)))
                .thenAnswer(call -> new TranslationService.Translation(
                        "[en]" + call.getArgument(0), "mock", glossaryVersion));
    }

    @Test
    void keepsStructureAndInlineTags() {
        echoTranslation(1);
        String html = "<h2>商品亮点</h2>"
                + "<p>这是一款<strong>陶瓷</strong>马克杯，容量 <em>350ml</em>。</p>"
                + "<ul><li>可微波炉加热</li><li>可洗碗机清洗</li></ul>"
                + "<figure><img src=\"/assets/a.png\" alt=\"杯子\"><figcaption>杯身细节</figcaption></figure>"
                + "<video src=\"/assets/v.mp4\" controls></video>";

        var result = service.translateHtml(html, "en", 1);

        assertThat(result.html()).contains("<h2>[en]商品亮点</h2>");
        assertThat(result.html()).contains("<strong>[en]陶瓷</strong>");
        assertThat(result.html()).contains("<em>[en]350ml</em>");
        assertThat(result.html()).contains("<li>[en]可微波炉加热</li>");
        assertThat(result.html()).contains("<figcaption>[en]杯身细节</figcaption>");
        // 图片/视频与属性原样保留
        assertThat(result.html()).contains("src=\"/assets/a.png\"");
        assertThat(result.html()).contains("alt=\"杯子\"");
        assertThat(result.html()).contains("controls");
        // 结构标签数量不变
        assertThat(result.html().split("<li>", -1)).hasSize(3);
        // 段的数量 = 文本节点数量（行内标签会把一段句子拆成多个节点，例如 "这是一款" / "陶瓷" / "马克杯，容量 " …）
        // 这正是想复用的粒度：改一个词只重翻那个节点
        assertThat(result.segments()).isEqualTo(9);
    }

    @Test
    void skipsCodeAndNoTranslateBlocks() {
        echoTranslation(1);
        String html = "<p>普通文本</p>"
                + "<p data-no-translate>Full Mall Pro</p>"
                + "<pre><code>SKU-123</code></pre>"
                + "<p>品牌 <span data-no-translate>Acme</span> 出品</p>";

        var result = service.translateHtml(html, "en", 1);

        assertThat(result.html()).contains(">Full Mall Pro<");
        assertThat(result.html()).contains("SKU-123");
        assertThat(result.html()).contains(">Acme<");
        assertThat(result.html()).contains("[en]普通文本");
        // 只翻了"普通文本"与"品牌"/"出品"两个文本节点
        assertThat(result.segments()).isEqualTo(3);
    }

    @Test
    void preservesSurroundingWhitespaceAndCountsMemoryHits() {
        when(translator.translate(anyString(), eq("en"), eq(2)))
                .thenReturn(new TranslationService.Translation("Milk", "memory", 2));
        var result = service.translateHtml("<p>  牛奶  </p>", "en", 2);

        assertThat(result.html()).contains("  Milk  ");
        assertThat(result.cached()).isEqualTo(1);
    }

    @Test
    void emptyHtmlIsFine() {
        var result = service.translateHtml("", "en", 1);
        assertThat(result.html()).isEmpty();
        assertThat(result.segments()).isZero();
    }
}
