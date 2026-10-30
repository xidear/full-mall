package com.fullmall.platform.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 导出文件清理：只删过期文件，保留新文件；天数非法直接拒绝。 */
class ExportFileCleanupTest {

    @Test
    void deletesOnlyOldFiles(@TempDir Path dir) throws IOException {
        var storage = new LocalExportStorage(dir);
        storage.store("old.csv", "a,b".getBytes());
        storage.store("new.csv", "c,d".getBytes());

        Path old = dir.resolve("old.csv");
        Files.setLastModifiedTime(old,
                FileTime.from(Instant.now().minus(Duration.ofDays(30))));

        int removed = storage.deleteOlderThan(7);

        assertThat(removed).isEqualTo(1);
        assertThat(Files.exists(old)).isFalse();
        assertThat(Files.exists(dir.resolve("new.csv"))).isTrue();
    }

    @Test
    void rejectsNonPositiveDays(@TempDir Path dir) {
        var storage = new LocalExportStorage(dir);

        assertThatThrownBy(() -> storage.deleteOlderThan(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("保留天数");
    }

    @Test
    void returnsZeroWhenDirectoryMissing(@TempDir Path dir) {
        var storage = new LocalExportStorage(dir.resolve("not-created"));

        assertThat(storage.deleteOlderThan(7)).isZero();
    }
}
