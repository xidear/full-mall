package com.fullmall.legal.domain;

import java.time.Instant;
import java.util.UUID;

public record LegalDocument(
        UUID id,
        String type,
        String version,
        String locale,
        String market,
        String title,
        String content,
        String contentHash,
        Instant effectiveFrom,
        String pdfUrl
) {
}