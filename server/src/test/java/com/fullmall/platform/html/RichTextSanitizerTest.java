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

    @Test
    void keepsTableStructureAndMergedCells() {
        String html = "<table><tbody><tr><th colspan=\"2\">参数</th></tr>"
                + "<tr><td>尺寸</td><td>183cm</td></tr></tbody></table>";

        String cleaned = sanitizer.sanitize(html);

        assertThat(cleaned).contains("<table>", "<th colspan=\"2\">参数</th>", "<td>尺寸</td>");
    }

    @Test
    void keepsAssetIdOnMedia() {
        // 前端插入素材库图片时会带 data-asset-id；被清洗掉就只剩 URL，服务端只能靠字符串比对归属
        String html = "<img src=\"/assets/assets/202610/a.png\" data-asset-id=\"11111111-1111-1111-1111-111111111111\">";

        String cleaned = sanitizer.sanitize(html);

        assertThat(cleaned).contains("data-asset-id=\"11111111-1111-1111-1111-111111111111\"");
    }

    @Test
    void keepsOnlyTextAlignInStyle() {
        String html = "<p style=\"position:fixed;top:0;text-align:center\">居中且不该盖住按钮</p>";

        String cleaned = sanitizer.sanitize(html);

        assertThat(cleaned).contains("style=\"text-align:center\"");
        assertThat(cleaned).doesNotContain("position", "top:0");
    }

    @Test
    void dropsStyleThatIsNotTextAlign() {
        String html = "<p style=\"background:url(https://evil.example/x.png)\">探针</p>";

        String cleaned = sanitizer.sanitize(html);

        assertThat(cleaned).doesNotContain("style=", "background", "evil.example");
        assertThat(cleaned).contains("探针");
    }
}
