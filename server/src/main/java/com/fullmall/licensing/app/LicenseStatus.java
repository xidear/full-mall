package com.fullmall.licensing.app;

import java.time.Instant;
import java.util.List;

/**
 * 许可状态。
 *
 * @param code   ACTIVE / TRIAL / EXPIRED / INVALID / UNLICENSED / CLOCK_ROLLBACK / DOMAIN_MISMATCH
 * @param readOnly 是否进入**只读模式**（GET 放行，写操作拒绝）
 */
public record LicenseStatus(
        String code,
        String message,
        String customer,
        String edition,
        List<String> features,
        Instant expiresAt,
        boolean readOnly) {

    public boolean isReadOnly() {
        return readOnly;
    }
}
