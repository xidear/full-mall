package com.fullmall.legal.api;

import java.time.Instant;
import java.util.UUID;

public record LegalAcceptance(
        UUID id,
        UUID documentVersionId,
        String locale,
        Instant acceptedAt,
        String evidenceHash
) {
}