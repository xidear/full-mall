package com.fullmall.legal.app;

import com.fullmall.legal.api.LegalAcceptance;
import com.fullmall.legal.api.LegalAcceptanceRequest;
import com.fullmall.legal.domain.LegalDocument;
import com.fullmall.legal.infra.LegalRepository;
import com.fullmall.platform.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class LegalService {

    private final LegalRepository repository;

    public LegalService(LegalRepository repository) {
        this.repository = repository;
    }

    public LegalDocument findActive(String type, String locale, String market) {
        LegalDocument document = repository.findActive(type, locale, market);
        if (document == null) {
            throw ApiException.notFound("LEGAL_DOCUMENT_NOT_FOUND",
                    "No published legal document for type=" + type + ", locale=" + locale + ", market=" + market);
        }
        return document;
    }

    @Transactional
    public LegalAcceptance accept(LegalAcceptanceRequest request, String ip, String userAgent) {
        if (!repository.publishedVersionExists(request.documentVersionId())) {
            throw ApiException.badRequest("LEGAL_VERSION_NOT_PUBLISHED",
                    "Legal document version is not published: " + request.documentVersionId());
        }
        Instant acceptedAt = Instant.now();
        String evidence = sha256(String.join("|",
                request.documentVersionId().toString(),
                request.locale(),
                acceptedAt.toString(),
                ip == null ? "" : ip,
                userAgent == null ? "" : userAgent,
                request.method()));
        return repository.insertAcceptance(
                request.documentVersionId(),
                request.locale(),
                acceptedAt,
                ip,
                userAgent,
                request.method(),
                evidence,
                request.orderId(),
                request.sessionId());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}