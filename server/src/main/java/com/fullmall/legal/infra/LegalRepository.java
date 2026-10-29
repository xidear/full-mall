package com.fullmall.legal.infra;

import com.fullmall.legal.api.LegalAcceptance;
import com.fullmall.legal.domain.LegalDocument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class LegalRepository {

    private final JdbcTemplate jdbcTemplate;

    public LegalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 取当前生效版本：只认 PUBLISHED 且在生效窗口内（{@code effective_to} 为空表示长期有效），
     * 多条命中时取生效时间最新的那条。
     *
     * <p>返回值语义：{@code null} 表示"该 type/locale/market 组合当前没有生效版本"（合规配置缺失），
     * 调用方要按缺失处理并报错，不能当成"没有法律文本要求"就放过——那会让用户在无条款状态下下单。
     *
     * <p>{@code pdfUrl} 在本查询里恒为 null：当前只存 HTML 正文，PDF 由渲染侧生成。
     */
    public LegalDocument findActive(String type, String locale, String market) {
        List<LegalDocument> documents = jdbcTemplate.query("""
                SELECT v.id, d.type, v.version, l.locale, d.market_code, l.title, l.content,
                       l.content_hash, v.effective_from, v.effective_to
                FROM legal_document d
                JOIN legal_document_version v ON v.document_id = d.id
                JOIN legal_document_localization l ON l.version_id = v.id
                WHERE d.type = ?
                  AND d.market_code = ?
                  AND l.locale = ?
                  AND v.status = 'PUBLISHED'
                  AND v.effective_from <= now()
                  AND (v.effective_to IS NULL OR v.effective_to > now())
                ORDER BY v.effective_from DESC
                LIMIT 1
                """, (rs, rowNum) -> new LegalDocument(
                rs.getObject("id", UUID.class),
                rs.getString("type"),
                rs.getString("version"),
                rs.getString("locale"),
                rs.getString("market_code"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("content_hash"),
                rs.getTimestamp("effective_from").toInstant(),
                rs.getTimestamp("effective_to") == null
                        ? null : rs.getTimestamp("effective_to").toInstant(),
                null), type, market, locale);
        return documents.stream().findFirst().orElse(null);
    }

    /**
     * 该版本是否"已发布"。
     *
     * <p>返回 false 同时覆盖"版本不存在"和"存在但未发布"两种情况：调用方若要区分，
     * 必须自己再查一次；不要把它当成"版本是否存在"的判定用（空结果 ≠ 不存在）。
     */
    public boolean publishedVersionExists(UUID versionId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM legal_document_version
                WHERE id = ? AND status = 'PUBLISHED'
                """, Long.class, versionId);
        if (count == null) {
            throw new IllegalStateException("Legal version existence query returned null");
        }
        return count > 0;
    }

    /**
     * 记录一次"用户已阅读并同意"，返回落库后的证据行。
     *
     * <p>{@code user_id} 恒为 NULL：同意发生在注册/下单前，此时可能还没有会员身份，
     * 因此证据靠 {@code session_id + order_id + evidence_hash} 关联，而不是靠会员外键——
     * 不要因为看到 NULL 就以为漏填。
     *
     * <p>只插入不更新：同意记录是争议时的证据链，改写等于篡改；同一用户多次同意即多行。
     */
    public LegalAcceptance insertAcceptance(UUID versionId, String locale, Instant acceptedAt,
                                            String ip, String userAgent, String method,
                                            String evidenceHash, UUID orderId, String sessionId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO legal_acceptance (id, user_id, document_version_id, locale, accepted_at,
                                              ip, user_agent, method, evidence_hash, order_id, session_id)
                VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, versionId, locale, java.sql.Timestamp.from(acceptedAt), ip, userAgent, method,
                evidenceHash, orderId, sessionId);
        return new LegalAcceptance(id, versionId, locale, acceptedAt, evidenceHash);
    }
}