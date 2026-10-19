package com.fullmall.platform.media;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaUrlResolverTest {

    private final MediaUrlResolver resolver =
            new MediaUrlResolver("https://static.ccc.com");

    @Test
    void absoluteUrlIsKeptForHistoricalSnapshots() {
        assertThat(resolver.absolute("https://www.aaa.com/bbb.jpg"))
                .isEqualTo("https://www.aaa.com/bbb.jpg");
    }

    @Test
    void relativePathIsResolvedAgainstConfiguredBase() {
        assertThat(resolver.absolute("/images/aaa.jpg")).isEqualTo("https://static.ccc.com/images/aaa.jpg");
        assertThat(resolver.absolute("images/aaa.jpg")).isEqualTo("https://static.ccc.com/images/aaa.jpg");
    }

    @Test
    void baseUrlTrailingSlashDoesNotDoubleUp() {
        var withSlash = new MediaUrlResolver("https://static.ccc.com/");
        assertThat(withSlash.absolute("images/a.jpg")).isEqualTo("https://static.ccc.com/images/a.jpg");
    }

    @Test
    void protocolRelativeBecomesHttps() {
        assertThat(resolver.absolute("//cdn.example.com/a.jpg")).isEqualTo("https://cdn.example.com/a.jpg");
    }

    @Test
    void pathTraversalIsRejected() {
        assertThatThrownBy(() -> resolver.absolute("../images/aaa.jpg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("..");
    }

    @Test
    void missingBaseFailsLoudlyInsteadOfStoringRelativePath() {
        var unconfigured = new MediaUrlResolver("");
        assertThatThrownBy(() -> unconfigured.absolute("images/a.jpg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("public-base-url");
    }

    @Test
    void blankStaysNull() {
        assertThat(resolver.absolute(null)).isNull();
        assertThat(resolver.absolute("  ")).isNull();
    }
}
