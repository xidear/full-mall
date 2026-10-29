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

    /**
     * 翻译一段文本，先命中翻译记忆再调用 Provider。
     *
     * <p>记忆键是「源文 SHA-256 + locale + 术语表版本」：同一句重复翻译不再计费，
     * 而改了术语表会天然失效旧记忆（版本号变了就查不到）。
     *
     * <p>Provider 失败时异常直接抛出，不返回原文兜底——把未翻译的中文当译文写进草稿，
     * 会让人误以为翻译成功。返回值的 {@code source} 标明来自 {@code memory} 还是具体 Provider，
     * 便于审计与成本核对。
     */
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
        // 并发下同一句可能被两个请求同时翻译：ON CONFLICT DO NOTHING 让后写的静默放弃，
        // 避免唯一键冲突把一次已经付费成功的翻译判成失败。
        jdbcTemplate.update("""
                INSERT INTO translation_memory (source_hash, locale, glossary_version, translated_text, provider)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (source_hash, locale, glossary_version) DO NOTHING
                """, hash, locale, glossaryVersion, translated, provider.name());
        return new Translation(translated, provider.name(), glossaryVersion);
    }

    /** 当前术语表版本：术语变化会影响机翻结果，记忆键必须带上版本，否则改了术语仍命中旧译文。 */
    public int currentGlossaryVersion() {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version), 1) FROM translation_glossary", Integer.class);
        return version == null ? 1 : version;
    }

    /** 源文指纹：标题 + 描述（含分隔符），用于 OUTDATED 判定。 */
    public String sourceHash(String title, String description) {
        return sha256Hex((title == null ? "" : title.trim()) + "\u0000"
                + (description == null ? "" : description.trim()));
    }

    private Optional<String> findMemory(String hash, String locale, int glossaryVersion) {
        List<String> rows = jdbcTemplate.query("""
                SELECT translated_text FROM translation_memory
                 WHERE source_hash = ? AND locale = ? AND glossary_version = ?
                """, (rs, rowNum) -> rs.getString("translated_text"), hash, locale, glossaryVersion);
        return rows.stream().findFirst();
    }

    // 按 source_term 长度倒序替换：短词先替换会把长词拆坏（“内存”→“内”），
    // 长词优先才能让“内存条”“内存”这类包含关系保持正确。只做整词字面替换，不猜语法。
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