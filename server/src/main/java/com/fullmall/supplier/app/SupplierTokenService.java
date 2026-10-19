package com.fullmall.supplier.app;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 供应商 Bearer Token（SUP-030 的第二条认证路径）。
 *
 * <p>安全约束：
 * <ul>
 *   <li>只存 **SHA-256 哈希**，明文仅在签发响应里出现一次；</li>
 *   <li>过期与吊销在验证时逐条判断，不依赖缓存；</li>
 *   <li>验证失败一律返回 empty，由调用方统一回 401，不区分“不存在/已过期/已吊销”，避免枚举。</li>
 * </ul>
 */
@Service
public class SupplierTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final JdbcTemplate jdbcTemplate;

    public SupplierTokenService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    /**
     * 签发新 token：覆盖旧的 token（轮换即失效）。
     */
    public IssuedToken issue(UUID supplierId, Duration ttl) {
        if (supplierId == null) {
            throw new IllegalArgumentException("supplierId 不能为空");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("token 有效期必须大于 0，实际=" + ttl);
        }
        Long exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM supplier_credential WHERE supplier_id = ?", Long.class, supplierId);
        if (exists == null || exists == 0) {
            throw new IllegalStateException("供应商没有凭证记录，无法签发 token: " + supplierId);
        }

        byte[] raw = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        Instant expiresAt = Instant.now().plus(ttl);

        int updated = jdbcTemplate.update("""
                UPDATE supplier_credential
                   SET token_hash = ?, token_expires_at = ?, token_revoked_at = NULL, active = TRUE
                 WHERE supplier_id = ?
                """, sha256Hex(token), Timestamp.from(expiresAt), supplierId);
        if (updated != 1) {
            throw new IllegalStateException("token 写入失败: supplierId=" + supplierId + "，影响行数=" + updated);
        }
        return new IssuedToken(token, expiresAt);
    }

    public record CredentialState(String authMode, String ipAllowlist, Instant tokenExpiresAt,
                                  boolean tokenRevoked, boolean active) {
    }

    public CredentialState state(UUID supplierId) {
        List<CredentialState> rows = jdbcTemplate.query("""
                SELECT auth_mode, ip_allowlist, token_expires_at, token_revoked_at, active
                  FROM supplier_credential WHERE supplier_id = ?
                """, (rs, rowNum) -> new CredentialState(
                rs.getString("auth_mode"),
                rs.getString("ip_allowlist"),
                rs.getTimestamp("token_expires_at") == null
                        ? null : rs.getTimestamp("token_expires_at").toInstant(),
                rs.getTimestamp("token_revoked_at") != null,
                rs.getBoolean("active")), supplierId);
        return rows.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("供应商没有凭证记录: " + supplierId));
    }

    public void revoke(UUID supplierId) {
        int updated = jdbcTemplate.update("""
                UPDATE supplier_credential SET token_revoked_at = now() WHERE supplier_id = ?
                """, supplierId);
        if (updated != 1) {
            throw new IllegalStateException("token 吊销失败: supplierId=" + supplierId + "，影响行数=" + updated);
        }
    }

    /**
     * 校验 token：命中且未过期、未吊销、且认证模式允许 token。
     */
    public Optional<UUID> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        List<UUID> matches = jdbcTemplate.query("""
                SELECT supplier_id FROM supplier_credential
                 WHERE token_hash = ?
                   AND token_revoked_at IS NULL
                   AND token_expires_at > now()
                   AND active = TRUE
                   AND auth_mode IN ('TOKEN_ONLY', 'BOTH')
                """, (rs, rowNum) -> rs.getObject("supplier_id", UUID.class), sha256Hex(token));
        return matches.stream().findFirst();
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