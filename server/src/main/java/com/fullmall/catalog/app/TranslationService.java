package com.fullmall.catalog.app;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * 翻译服务（I18N-003）：**先查翻译记忆，未命中才调用 Provider**，避免重复计费。
 *
 * <p>术语表优先级高于机翻：命中术语表时直接使用目标术语（这里只做整词替换，不臆造语法）。
 */
@Service
public class TranslationService {

    private final JdbcTemplate jdbcTemplate;
    private final TranslationProvider provider;

    public TranslationService(JdbcTemplate jdbcTemplate, TranslationProvider provider) {
        this.jdbcTemplate = jdbcTemplate;
        this.provider = provider;
    }

    public record Translation(String text, String source, int glossaryVersion) {
    }

    public Translation translate(String sourceText, String locale, int glossaryVersion) {
        if (sourceText == null || sourceText.isBlank()) {
            throw new IllegalArgumentException("源文不能为空");
        }
        if (locale == null || locale.isBlank()) {
            throw new IllegalArgumentException("目标语言不能为空");
        }

        String hash = sha256Hex(sourceText);
        Optional<String> cached = findMemory(hash, locale, glossaryVersion);
        if (cached.isPresent()) {
            return new Translation(cached.get(), "memory", glossaryVersion);
        }

        String translated = provider.translate(applyGlossary(sourceText, locale), locale);
        jdbcTemplate.update("""
                INSERT INTO translation_memory (source_hash, locale, glossary_version, translated_text, provider)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (source_hash, locale, glossary_version) DO NOTHING
                """, hash, locale, glossaryVersion, translated, provider.name());
        return new Translation(translated, provider.name(), glossaryVersion);
    }

    private Optional<String> findMemory(String hash, String locale, int glossaryVersion) {
        List<String> rows = jdbcTemplate.query("""
                SELECT translated_text FROM translation_memory
                 WHERE source_hash = ? AND locale = ? AND glossary_version = ?
                """, (rs, rowNum) -> rs.getString("translated_text"), hash, locale, glossaryVersion);
        return rows.stream().findFirst();
    }

    private String applyGlossary(String sourceText, String locale) {
        List<String[]> terms = jdbcTemplate.query("""
                SELECT source_term, target_term FROM translation_glossary WHERE locale = ?
                ORDER BY length(source_term) DESC
                """, (rs, rowNum) -> new String[]{rs.getString("source_term"), rs.getString("target_term")},
                locale);
        String result = sourceText;
        for (String[] term : terms) {
            result = result.replace(term[0], term[1]);
        }
        return result;
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 不可用", error);
        }
    }
}