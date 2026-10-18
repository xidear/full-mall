package com.fullmall.theme.domain;

import java.time.Instant;
import java.util.UUID;

public record ThemeConfigRecord(
        UUID id,
        String channel,
        String market,
        String configJson,
        int version,
        Instant updatedAt
) {
}