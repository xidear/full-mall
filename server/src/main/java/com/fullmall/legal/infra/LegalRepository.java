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

    public LegalDocument findActive(String type, String locale, String market) {
        List<LegalDocument> documents = jdbcTemplate.query("""
                SELECT v.id, d.type, v.version, l.locale, d.market_code, l.title, l.content,
                       l.content_hash, v.effective_from
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
                null), type, market, locale);
        return documents.stream().findFirst().orElse(null);
    }

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