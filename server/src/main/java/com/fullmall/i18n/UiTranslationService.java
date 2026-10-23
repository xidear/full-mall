package com.fullmall.i18n;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 多语言文案（H9）。
 *
 * <p>规则：
 * <ul>
 *   <li>支持语言白名单：`zh-CN`、`en`（后续按需扩充）；</li>
 *   <li>取文案时**缺失回退 zh-CN**，仍缺则返回 key 本身（绝不返回空串让界面显示空白）；</li>
 *   <li>写入只允许白名单语言与非空 key/value。</li>
 * </ul>
 */
@Service
public class UiTranslationService {

    public static final Set<String> SUPPORTED = Set.of("zh-CN", "en");
    private static final String FALLBACK = "zh-CN";

    private final JdbcTemplate jdbc;

    public UiTranslationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, String> bundle(String locale) {
        String normalized = normalize(locale);
        Map<String, String> result = new LinkedHashMap<>(
                read(SUPPORTED.contains(normalized) ? normalized : FALLBACK));
        if (!FALLBACK.equals(normalized)) {
            // 缺失的键回退到中文，保证界面不出现空白
            read(FALLBACK).forEach(result::putIfAbsent);
        }
        return result;
    }

    public String text(String locale, String key) {
        Map<String, String> bundle = bundle(locale);
        return bundle.getOrDefault(key, key);
    }

    @Transactional
    public void upsert(String locale, Map<String, String> entries) {
        String normalized = normalize(locale);
        if (!SUPPORTED.contains(normalized)) {
            throw ApiException.badRequest("I18N_LOCALE_UNSUPPORTED",
                    "不支持的语言：" + locale + "（可选 " + SUPPORTED + "）");
        }
        if (entries == null || entries.isEmpty()) {
            throw ApiException.badRequest("I18N_EMPTY", "没有需要更新的文案");
        }
        for (var entry : entries.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw ApiException.badRequest("I18N_KEY_REQUIRED", "文案 key 不能为空");
            }
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                throw ApiException.badRequest("I18N_VALUE_REQUIRED",
                        "文案值不能为空：" + entry.getKey());
            }
            jdbc.update("""
                    INSERT INTO ui_translation (locale, text_key, text_value, updated_at)
                    VALUES (?, ?, ?, now())
                    ON CONFLICT (locale, text_key)
                    DO UPDATE SET text_value = EXCLUDED.text_value, updated_at = now()
                    """, normalized, entry.getKey().trim(), entry.getValue().trim());
        }
    }

    /** 各语言覆盖率（缺多少 key），便于运营判断翻译进度。 */
    public List<Map<String, Object>> coverage() {
        return jdbc.query("""
                SELECT locale, COUNT(*) AS keys,
                       (SELECT COUNT(*) FROM ui_translation WHERE locale = 'zh-CN') AS base_keys
                  FROM ui_translation
                 GROUP BY locale
                 ORDER BY locale
                """, (rs, rowNum) -> Map.of(
                "locale", rs.getString("locale"),
                "keys", rs.getLong("keys"),
                "baseKeys", rs.getLong("base_keys")));
    }

    private Map<String, String> read(String locale) {
        Map<String, String> map = new LinkedHashMap<>();
        jdbc.query("SELECT text_key, text_value FROM ui_translation WHERE locale = ? ORDER BY text_key",
                rs -> {
                    map.put(rs.getString("text_key"), rs.getString("text_value"));
                }, locale);
        return map;
    }

    private String normalize(String locale) {
        String value = locale == null ? "" : locale.trim();
        if (value.isEmpty()) {
            return FALLBACK;
        }
        for (String supported : SUPPORTED) {
            if (supported.equalsIgnoreCase(value)) {
                return supported;
            }
        }
        return value.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
