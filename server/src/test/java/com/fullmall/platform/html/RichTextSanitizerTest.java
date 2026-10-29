package com.fullmall.platform.html;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 富文本白名单：这条用例守住"运营可编辑 HTML 不能变成 XSS 入口"。
 *
 * <p>前端编辑器（TipTap）只是体验层，请求可以绕过它直接 PUT；
 * 服务端清洗才是权威，所以这里断言 script/事件属性/非白名单协议都被去掉。
 */
class RichTextSanitizerTest {

    private final RichTextSanitizer sanitizer = new RichTextSanitizer();

    @Test
    void keepsAllowedStructure() {
        String html = "<h2>材质</h2><p>这是 <strong>加粗</strong> 与 <em>斜体</em>。</p>"
                + "<ul><li>一条</li></ul><figure><img src=\"/images/a.png\" alt=\"图\">"
                + "<figcaption>说明</figcaption></figure><video src=\"/assets/v.mp4\" controls></video>";

        String cleaned = sanitizer.sanitize(html);

        assertThat(cleaned).contains("<h2>材质</h2>", "<strong>加粗</strong>", "<em>斜体</em>",
                "<li>一条</li>", "<figcaption>说明</figcaption>", "controls");
        assertThat(cleaned).contains("src=\"/images/a.png\"");
    }

    @Test
    void stripsScriptsEventHandlersAndDangerousProtocols() {
        String html = "<p>正常</p><script>alert(1)</script>"
                + "<img src=\"javascript:alert(2)\">"
                + "<p onclick=\"steal()\">带事件</p>"
                + "<a href=\"javascript:alert(3)\">链接</a>";

        String cleaned = sanitizer.sanitize(html);

        assertThat(cleaned).doesNotContain("script", "onclick", "javascript:");
        assertThat(cleaned).contains("正常").contains("带事件");
    }

    @Test
    void blankInputStaysBlank() {
        assertThat(sanitizer.sanitize("")).isEmpty();
        assertThat(sanitizer.sanitize(null)).isNull();
    }
}
