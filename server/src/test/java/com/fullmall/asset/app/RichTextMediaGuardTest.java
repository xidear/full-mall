package com.fullmall.asset.app;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 素材归属判定：这条用例守住"富文本里的图必须来自素材库"。
 *
 * <p>不清洗库、不起 Spring：只测纯判定规则（missingLibraryMedia），
 * 因为规则本身才是业务边界；SQL 部分只是把库里的 url/id 喂进来。
 */
class RichTextMediaGuardTest {

    private static final Set<String> KNOWN_URLS = Set.of("/assets/assets/202610/a.png", "/assets/assets/202610/b.mp4");
    private static final Set<String> KNOWN_IDS = Set.of("11111111-1111-1111-1111-111111111111");

    private List<String> check(String html) {
        return RichTextMediaGuard.missingLibraryMedia(html, KNOWN_URLS::contains, KNOWN_IDS::contains);
    }

    @Test
    void acceptsLibraryUrlAndAssetId() {
        String html = "<p>说明</p><img src=\"/assets/assets/202610/a.png\" data-asset-id=\"11111111-1111-1111-1111-111111111111\">";
        assertThat(check(html)).isEmpty();
    }

    @Test
    void acceptsBundledDemoImage() {
        assertThat(check("<img src=\"/images/demo.png\">")).isEmpty();
    }

    @Test
    void rejectsExternalAndBase64Media() {
        List<String> missing = check(
                "<img src=\"https://cdn.example.com/x.png\"><img src=\"data:image/png;base64,AAAA\">");
        assertThat(missing).containsExactly("https://cdn.example.com/x.png", "data:image/png;base64,AAAA");
    }

    @Test
    void rejectsAssetIdThatDoesNotExist() {
        assertThat(check("<img src=\"/assets/assets/202610/zzz.png\" data-asset-id=\"22222222-2222-2222-2222-222222222222\">"))
                .containsExactly("/assets/assets/202610/zzz.png");
    }

    @Test
    void acceptsIdWhenCdnRewroteUrl() {
        // 素材换 CDN 后 url 与库里不一致，但 data-asset-id 仍能对上：不能误判为"不在素材库"
        assertThat(check("<img src=\"https://cdn.example.com/a.png\" data-asset-id=\"11111111-1111-1111-1111-111111111111\">"))
                .isEmpty();
    }

    @Test
    void checksElementWithoutSrc() {
        assertThat(check("<img data-asset-id=\"22222222-2222-2222-2222-222222222222\">")).isNotEmpty();
    }

    @Test
    void blankInputPasses() {
        assertThat(check(null)).isEmpty();
        assertThat(check("   ")).isEmpty();
        assertThat(check("<p>纯文字</p>")).isEmpty();
    }
}
