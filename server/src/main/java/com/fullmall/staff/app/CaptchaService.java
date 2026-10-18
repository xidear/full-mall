package com.fullmall.staff.app;

import com.fullmall.staff.api.CaptchaResponse;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConcurrentHashMap<String, Challenge> challenges = new ConcurrentHashMap<>();

    public CaptchaResponse create() {
        int left = 1 + RANDOM.nextInt(9);
        int right = 1 + RANDOM.nextInt(9);
        String id = UUID.randomUUID().toString();
        challenges.put(id, new Challenge(String.valueOf(left + right), Instant.now().plus(TTL)));
        return new CaptchaResponse(id, left + " + " + right + " = ?");
    }

    public boolean verify(String captchaId, String answer) {
        if (captchaId == null || answer == null) {
            return false;
        }
        Challenge challenge = challenges.remove(captchaId);
        if (challenge == null) {
            return false;
        }
        return challenge.expiresAt().isAfter(Instant.now())
                && challenge.answer().equals(answer.trim());
    }

    private record Challenge(String answer, Instant expiresAt) {
    }
}