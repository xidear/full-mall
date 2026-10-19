package com.fullmall.member.auth;

import com.fullmall.platform.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * 会员手机 OTP 登录（MEM-001）。
 *
 * <p>安全要点：
 * <ul>
 *   <li>验证码 **BCrypt 存储**，不落明文；</li>
 *   <li>5 分钟过期、最多 5 次尝试、用后即失效；</li>
 *   <li>会话 token 只存 SHA-256 哈希；</li>
 *   <li>演示/开发环境可通过 `app.member.otp.return-code=true` 把验证码回传（仅 Mock，短信通道未接），
 *       生产必须为 false——否则等于把验证码公开。</li>
 * </ul>
 */
@Service
public class MemberAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    private final JdbcTemplate jdbcTemplate;
    private final OtpAttemptRecorder attemptRecorder;
    private final boolean returnCode;

    public MemberAuthService(JdbcTemplate jdbcTemplate, OtpAttemptRecorder attemptRecorder,
                             @Value("${app.member.otp.return-code:false}") boolean returnCode) {
        this.jdbcTemplate = jdbcTemplate;
        this.attemptRecorder = attemptRecorder;
        this.returnCode = returnCode;
    }

    public record OtpIssued(String phone, String code, Instant expiresAt) {
    }

    public record SessionIssued(String token, UUID memberId, String memberNo) {
    }

    @Transactional
    public OtpIssued requestOtp(String phone) {
        String normalized = normalizePhone(phone);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(OTP_TTL);
        jdbcTemplate.update("""
                INSERT INTO member_otp (id, phone, code_hash, expires_at)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), normalized, ENCODER.encode(code),
                java.sql.Timestamp.from(expiresAt));
        return new OtpIssued(normalized, returnCode ? code : null, expiresAt);
    }

    @Transactional
    public SessionIssued verifyOtp(String phone, String code) {
        String normalized = normalizePhone(phone);
        if (code == null || !code.matches("^[0-9]{6}$")) {
            throw ApiException.badRequest("OTP_INVALID_FORMAT", "验证码为 6 位数字");
        }

        List<OtpRow> rows = jdbcTemplate.query("""
                SELECT id, code_hash, attempts, expires_at FROM member_otp
                 WHERE phone = ? AND consumed_at IS NULL
                 ORDER BY created_at DESC LIMIT 1
                """, (rs, rowNum) -> new OtpRow(
                rs.getObject("id", UUID.class), rs.getString("code_hash"), rs.getInt("attempts"),
                rs.getTimestamp("expires_at").toInstant()), normalized);

        if (rows.isEmpty()) {
            throw ApiException.badRequest("OTP_NOT_REQUESTED", "请先获取验证码");
        }
        OtpRow row = rows.get(0);
        if (row.expiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("OTP_EXPIRED", "验证码已过期，请重新获取");
        }
        if (row.attempts() >= MAX_ATTEMPTS) {
            throw ApiException.conflict("OTP_TOO_MANY_ATTEMPTS", "尝试次数过多，请重新获取验证码");
        }
        if (!ENCODER.matches(code, row.codeHash())) {
            // 独立事务提交，避免随主事务回滚（否则失败尝试不计入限制）
            attemptRecorder.recordFailure(row.id());
            throw ApiException.badRequest("OTP_MISMATCH", "验证码不正确");
        }

        jdbcTemplate.update("UPDATE member_otp SET consumed_at = now() WHERE id = ?", row.id());
        UUID memberId = findOrCreateMember(normalized);
        String token = newToken();
        jdbcTemplate.update("""
                INSERT INTO member_session (token_hash, member_id, expires_at)
                VALUES (?, ?, ?)
                """, sha256Hex(token), memberId, java.sql.Timestamp.from(Instant.now().plus(Duration.ofDays(30))));
        String memberNo = jdbcTemplate.queryForObject(
                "SELECT member_no FROM member WHERE id = ?", String.class, memberId);
        return new SessionIssued(token, memberId, memberNo);
    }

    public record MemberProfile(UUID id, String memberNo, String nameMasked, String phoneMasked,
                                String tier, long points, long storedValue, String currency) {
    }

    /** 会员自身资料（手机号与姓名掩码，避免前端/日志泄露 PII）。 */
    public MemberProfile profile(UUID memberId) {
        return jdbcTemplate.query("""
                SELECT id, member_no, name, phone, tier, points_balance, stored_value_balance, currency
                  FROM member WHERE id = ?
                """, (rs, rowNum) -> new MemberProfile(
                rs.getObject("id", UUID.class),
                rs.getString("member_no"),
                com.fullmall.platform.export.FieldMasker.name(rs.getString("name")),
                com.fullmall.platform.export.FieldMasker.phone(rs.getString("phone")),
                rs.getString("tier"),
                rs.getLong("points_balance"),
                rs.getLong("stored_value_balance"),
                rs.getString("currency")), memberId)
                .stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("MEMBER_NOT_FOUND", "会员不存在: " + memberId));
    }

    /** 会话 token → 会员 ID；无效返回 empty（由调用方决定 401）。 */
    public java.util.Optional<UUID> resolveSession(String token) {
        if (token == null || token.isBlank()) {
            return java.util.Optional.empty();
        }
        List<UUID> rows = jdbcTemplate.queryForList("""
                SELECT member_id FROM member_session WHERE token_hash = ? AND expires_at > now()
                """, UUID.class, sha256Hex(token.trim()));
        return rows.stream().findFirst();
    }

    private UUID findOrCreateMember(String phone) {
        List<UUID> existing = jdbcTemplate.queryForList(
                "SELECT id FROM member WHERE phone = ?", UUID.class, phone);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        UUID id = UUID.randomUUID();
        String memberNo = "M" + (System.currentTimeMillis() % 100_000_000L);
        jdbcTemplate.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance,
                                    stored_value_balance, currency)
                VALUES (?, ?, ?, ?, 'STANDARD', 0, 0, 'CNY')
                """, id, memberNo, "会员" + phone.substring(phone.length() - 4), phone);
        return id;
    }

    private String normalizePhone(String phone) {
        if (phone == null || !phone.trim().matches("^\\+?[0-9]{6,20}$")) {
            throw ApiException.badRequest("INVALID_PHONE", "手机号格式不正确");
        }
        return phone.trim();
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 不可用", error);
        }
    }

    private record OtpRow(UUID id, String codeHash, int attempts, Instant expiresAt) {
    }
}
