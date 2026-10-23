package com.fullmall.platform.paging;

import com.fullmall.platform.error.ApiException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * keyset 分页统一封装（此前 6 个模块各写一遍"解析游标 + 组装 next"）。
 *
 * <p>排序约定：`(created_at DESC, id DESC)`——与所有 keyset 查询的 `WHERE (created_at, id) < (?, ?)` 一致。
 * 游标由上一页最后一条记录给出，避免 offset 深页扫描。
 */
public final class KeysetPage {

    public static final int MAX_SIZE = 100;

    private KeysetPage() {
    }

    /** 游标；`createdAt` 为空表示第一页。 */
    public record Cursor(Instant createdAt, UUID id) {

        public static Cursor parse(String createdAt, UUID id) {
            if (createdAt == null || createdAt.isBlank()) {
                return new Cursor(null, null);
            }
            Instant instant;
            try {
                instant = Instant.parse(createdAt.trim());
            } catch (RuntimeException error) {
                throw ApiException.badRequest("CURSOR_INVALID",
                        "游标时间格式非法（需 ISO-8601）：" + createdAt);
            }
            if (id == null) {
                throw ApiException.badRequest("CURSOR_INCOMPLETE", "游标缺少 id，需与 createdAt 同时提供");
            }
            return new Cursor(instant, id);
        }
    }

    public static int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    /**
     * 组装响应：items + size + nextCreatedAt/nextId（无更多数据时两者为 null）。
     *
     * @param items    本页数据（按 created_at DESC, id DESC 排序）
     * @param size     已归一化的页大小
     * @param createdAtOf 取记录时间
     * @param idOf     取记录 id
     */
    public static <T> Map<String, Object> toResponse(List<T> items, int size,
                                                     Function<T, Instant> createdAtOf,
                                                     Function<T, UUID> idOf) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("size", size);
        if (items == null || items.isEmpty()) {
            result.put("nextCreatedAt", null);
            result.put("nextId", null);
            return result;
        }
        T last = items.get(items.size() - 1);
        result.put("nextCreatedAt", createdAtOf.apply(last).toString());
        result.put("nextId", idOf.apply(last));
        return result;
    }
}
