package com.fullmall.platform.asset;

import com.fullmall.platform.asset.local.LocalAssetStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 本地素材存储：写入可读、删除幂等、**路径穿越必须拒绝**。 */
class LocalAssetStorageTest {

    @Test
    void putAndDeleteRoundTrip(@TempDir Path dir) throws Exception {
        var storage = new LocalAssetStorage(dir.toString(), "https://cdn.example.com/");

        String url = storage.put("product/1/a.png", new byte[]{1, 2, 3}, "image/png");

        assertThat(url).isEqualTo("https://cdn.example.com/product/1/a.png");
        assertThat(Files.readAllBytes(dir.resolve("product/1/a.png"))).containsExactly(1, 2, 3);
        assertThat(storage.delete("product/1/a.png")).isTrue();
        assertThat(storage.delete("product/1/a.png")).as("删除幂等").isFalse();
    }

    @Test
    void pathTraversalIsRejected(@TempDir Path dir) {
        var storage = new LocalAssetStorage(dir.toString(), "https://cdn.example.com");

        assertThatThrownBy(() -> storage.put("../escape.txt", new byte[]{1}, "text/plain"))
                .hasMessageContaining("非法 key");
        assertThatThrownBy(() -> storage.put("/etc/passwd", new byte[]{1}, "text/plain"))
                .hasMessageContaining("非法 key");
    }
}
