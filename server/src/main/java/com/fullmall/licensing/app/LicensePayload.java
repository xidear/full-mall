package com.fullmall.licensing.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/** 许可证正文（签名覆盖其原始字节，字段顺序由签发方固定）。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LicensePayload(
        String licenseId,
        String customer,
        String edition,
        List<String> features,
        List<String> domains,
        Instant issuedAt,
        /** null 表示永久 */
        Instant expiresAt,
        int graceDays) {
}
