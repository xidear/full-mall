package com.fullmall.staff.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.staff.api.LoginResponse;
import com.fullmall.staff.domain.StaffAccount;
import com.fullmall.staff.domain.StaffSessionUser;
import com.fullmall.staff.domain.StaffUser;
import com.fullmall.staff.infra.StaffAuthRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 员工认证（app 层）：会话 token 只存 SHA-256 摘要，库被读走也换不回可用 token（靠 32 字节随机熵，不靠盐）。
 *
 * <p>失败计数在进程内 Map 里，属于"每实例"限流：重启清零、多副本不共享——挡住在线爆破没问题，
 * 挡不住分布式撞库。本类只回答"你是谁"；能做什么由角色与门店绑定在鉴权层裁决。
 */
@Service
public class StaffAuthService {

    private static final Duration SESSION_TTL = Duration.ofHours(8);
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final int MAX_FAILURES = 5;
    /** 与 docs/validation.schema.json 共享同一组规则。 */
    private static final int MIN_PASSWORD_LENGTH =
            com.fullmall.platform.validation.ValidationRules.PASSWORD_MIN_LENGTH;
    private static final int MAX_PIN_LENGTH =
            com.fullmall.platform.validation.ValidationRules.PIN_MAX_LENGTH;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    private final StaffAuthRepository repository;
    private final PasswordHasher passwordHasher;
    private final CaptchaService captchaService;
    private final com.fullmall.platform.audit.AuditEventService auditEvents;

    public StaffAuthService(StaffAuthRepository repository, PasswordHasher passwordHasher,
                            CaptchaService captchaService,
                            com.fullmall.platform.audit.AuditEventService auditEvents) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.captchaService = captchaService;
        this.auditEvents = auditEvents;
    }

    /**
     * 用户不存在、账号被停用、密码错误统一返回 INVALID_CREDENTIALS，避免被拿来枚举账号；
     * 计数仍按输入的用户名累加，连续 5 次锁 10 分钟，成功即清零。
     */
    @Transactional
    public LoginResponse login(String username, String password, String captchaId, String captchaAnswer) {
        if (!captchaService.verify(captchaId, captchaAnswer)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CAPTCHA_INVALID", "验证码错误或已过期");
        }
        String key = username == null ? "" : username.trim().toLowerCase();
        Attempt attempt = attempts.get(key);
        if (attempt != null && attempt.lockedUntil() != null && attempt.lockedUntil().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_LOCKED", "登录失败次数过多，请 10 分钟后再试");
        }

        StaffAccount account = repository.findAccountByUsername(username);
        if (account == null
                || !"ACTIVE".equals(account.user().status())
                || !passwordHasher.verify(password, account.passwordHash())) {
            int failures = attempt == null ? 1 : attempt.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? Instant.now().plus(LOCK_TTL) : null;
            attempts.put(key, new Attempt(failures, lockedUntil));
            String message = lockedUntil != null
                    ? "登录失败次数过多，请 10 分钟后再试"
                    : "用户名或密码错误，还可尝试 " + Math.max(0, MAX_FAILURES - failures) + " 次";
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", message);
        }
        attempts.remove(key);
        return issueSession(account.user());
    }

    public StaffUser me(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        String hash = tokenHash(rawToken);
        StaffSessionUser session = repository.findSession(hash);
        if (session == null) {
            return null;
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            repository.deleteSession(hash);
            return null;
        }
        if (!"ACTIVE".equals(session.user().status())) {
            return null;
        }
        return session.user();
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            repository.deleteSession(tokenHash(rawToken));
        }
    }

    @Transactional
    public StaffUser setPin(UUID staffId, String pin) {
        if (pin == null || pin.trim().length()
                < com.fullmall.platform.validation.ValidationRules.PIN_MIN_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PIN_TOO_SHORT", "PIN 至少 4 位");
        }
        if (pin.trim().length() > MAX_PIN_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PIN_TOO_LONG",
                    "PIN 最多 " + MAX_PIN_LENGTH + " 位");
        }
        StaffAccount account = repository.findAccountById(staffId);
        if (account == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "STAFF_NOT_FOUND", "员工不存在");
        }
        repository.updatePin(staffId, passwordHasher.hash(pin.trim()));
        return account.user();
    }

    /** PIN 与会话相互独立：账号停用、未设 PIN、PIN 错一律返回 null，不暴露"该账号有没有设置 PIN"。 */
    public StaffUser verifyPin(String username, String pin) {
        StaffAccount account = repository.findAccountByUsername(username);
        if (account == null || !"ACTIVE".equals(account.user().status())) {
            return null;
        }
        String hash = repository.findPinHash(account.user().id());
        if (hash == null || pin == null || !passwordHasher.verify(pin.trim(), hash)) {
            return null;
        }
        return account.user();
    }

    @Transactional
    public void bindStore(UUID staffId, UUID storeId, String actor) {
        if (repository.findAccountById(staffId) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "STAFF_NOT_FOUND", "员工不存在");
        }
        repository.bindStore(staffId, storeId);
        auditPermission("BIND", staffId, storeId, actor);
    }

    @Transactional
    public void unbindStore(UUID staffId, UUID storeId, String actor) {
        if (repository.findAccountById(staffId) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "STAFF_NOT_FOUND", "员工不存在");
        }
        repository.unbindStore(staffId, storeId);
        auditPermission("UNBIND", staffId, storeId, actor);
    }

    /** SEC-003：权限（门店绑定）变更必须留痕；缺操作人直接失败，不伪造。 */
    private void auditPermission(String action, UUID staffId, UUID storeId, String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("门店绑定变更必须记录操作人: staffId=" + staffId);
        }
        auditEvents.record(new com.fullmall.platform.audit.AuditEventService.AuditEvent(
                "PERMISSION_CHANGED", actor, "staff_store", staffId.toString(),
                action + " store=" + storeId, null));
    }

    public List<UUID> storeIds(UUID staffId) {
        return repository.findStoreIds(staffId);
    }

    /** 改密后删除该员工全部会话：旧 token 立即失效，避免共享设备上残留的会话继续可用。 */
    @Transactional
    public void changePassword(StaffUser user, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_SHORT",
                    "新密码至少 " + MIN_PASSWORD_LENGTH + " 位");
        }
        StaffAccount account = repository.findAccountByUsername(user.username());
        if (account == null || !passwordHasher.verify(oldPassword, account.passwordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OLD_PASSWORD_MISMATCH", "原密码不正确");
        }
        if (oldPassword.equals(newPassword)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_UNCHANGED", "新密码不能与原密码相同");
        }
        repository.updatePassword(user.id(), passwordHasher.hash(newPassword));
        repository.deleteSessionsForStaff(user.id());
    }

    private LoginResponse issueSession(StaffUser user) {
        String token = newToken();
        repository.insertSession(tokenHash(token), user.id(), Instant.now().plus(SESSION_TTL));
        // SEC-003：登录必须留痕（成功登录；失败由认证异常路径返回，不记录密码）
        auditEvents.record(new com.fullmall.platform.audit.AuditEventService.AuditEvent(
                "LOGIN_SUCCESS", user.username(), "staff_user", user.id().toString(),
                "role=" + user.role(), null));
        return new LoginResponse(token, user);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String tokenHash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record Attempt(int failures, Instant lockedUntil) {
    }
}