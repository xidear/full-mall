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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StaffAuthService {

    private static final Duration SESSION_TTL = Duration.ofHours(8);
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final int MAX_FAILURES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    private final StaffAuthRepository repository;
    private final PasswordHasher passwordHasher;
    private final CaptchaService captchaService;

    public StaffAuthService(StaffAuthRepository repository, PasswordHasher passwordHasher,
                            CaptchaService captchaService) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.captchaService = captchaService;
    }

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
    public void changePassword(StaffUser user, String oldPassword, String newPassword) {
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