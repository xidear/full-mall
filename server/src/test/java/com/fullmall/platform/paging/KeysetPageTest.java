package com.fullmall.platform.paging;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** keyset 统一封装：游标解析、页大小归一化、next 计算。 */
class KeysetPageTest {

    record Row(UUID id, Instant createdAt) {
    }

    @Test
    void parsesCursorAndNormalizesSize() {
        var cursor = KeysetPage.Cursor.parse("2026-10-20T10:00:00Z", UUID.randomUUID());
        assertThat(cursor.createdAt()).isEqualTo(Instant.parse("2026-10-20T10:00:00Z"));

        assertThat(KeysetPage.Cursor.parse(null, null).createdAt()).isNull();
        assertThat(KeysetPage.normalizeSize(0)).isEqualTo(1);
        assertThat(KeysetPage.normalizeSize(500)).isEqualTo(100);
    }

    @Test
    void rejectsMalformedCursors() {
        assertThatThrownBy(() -> KeysetPage.Cursor.parse("not-a-time", UUID.randomUUID()))
                .hasMessageContaining("游标时间格式非法");
        assertThatThrownBy(() -> KeysetPage.Cursor.parse("2026-10-20T10:00:00Z", null))
                .hasMessageContaining("游标缺少 id");
    }

    @Test
    void buildsNextCursorFromLastRow() {
        UUID id = UUID.randomUUID();
        var rows = List.of(new Row(UUID.randomUUID(), Instant.parse("2026-10-20T11:00:00Z")),
                new Row(id, Instant.parse("2026-10-20T10:00:00Z")));

        var response = KeysetPage.toResponse(rows, 20, Row::createdAt, Row::id);
        assertThat(response).containsEntry("nextId", id)
                .containsEntry("nextCreatedAt", "2026-10-20T10:00:00Z");

        var empty = KeysetPage.toResponse(List.<Row>of(), 20, Row::createdAt, Row::id);
        assertThat(empty).containsEntry("nextId", null);
    }
}
