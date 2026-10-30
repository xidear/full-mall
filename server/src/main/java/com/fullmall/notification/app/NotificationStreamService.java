package com.fullmall.notification.app;

import com.fullmall.platform.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 后台通知的 SSE 通道（ADR 待补：见 docs/adr 讨论）。
 *
 * <p>为什么用短期一次性 token：浏览器的 {@code EventSource} **不能带 Authorization 头**，
 * 只能把凭据放进 URL。直接把 JWT 塞进 URL 会进浏览器历史/代理日志，因此改为：
 * 先用 Bearer 调 {@code POST /admin/v1/notifications/stream-token} 换一个 5 分钟、单次、
 * 绑定员工的随机 token，再用它连流。
 *
 * <p>已知边界（必须让运维知道）：
 * <ul>
 *   <li>emitter 与 token 都存在**本进程内存**，多副本部署时 A 副本的推送到不了连着 B 副本的人 ——
 *       真要横向扩容必须接 Redis pub/sub；</li>
 *   <li>SSE 只是"更快"，丢了不影响正确性：前端断开后自动回落到 30s 轮询。</li>
 * </ul>
 */
@Service
public class NotificationStreamService {

    private static final Logger log = LoggerFactory.getLogger(NotificationStreamService.class);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, PendingToken> tokens = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private record PendingToken(String staffUsername, Instant expiresAt) {
    }

    /** 为当前员工签发一次性流 token。 */
    public Map<String, Object> issueToken(String staffUsername) {
        purgeExpiredTokens();
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = HexFormat.of().formatHex(raw);
        Instant expiresAt = Instant.now().plus(TOKEN_TTL);
        tokens.put(token, new PendingToken(staffUsername, expiresAt));
        return Map.of("token", token, "expiresAt", expiresAt.toString());
    }

    /** 连接流：token 一次有效，校验后立即作废。 */
    public SseEmitter subscribe(String token) {
        PendingToken pending = token == null ? null : tokens.remove(token);
        if (pending == null || pending.expiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("NOTIFICATION_STREAM_TOKEN_INVALID",
                    "流 token 无效或已过期：请重新获取（一次性、5 分钟有效）");
        }
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> remove(pending.staffUsername(), emitter));
        emitter.onTimeout(() -> remove(pending.staffUsername(), emitter));
        emitter.onError(error -> remove(pending.staffUsername(), emitter));
        emitters.computeIfAbsent(pending.staffUsername(), key -> new CopyOnWriteArrayList<>()).add(emitter);
        try {
            emitter.send(SseEmitter.event().name("ready").data(Map.of("staff", pending.staffUsername())));
        } catch (IOException error) {
            remove(pending.staffUsername(), emitter);
            throw ApiException.badRequest("NOTIFICATION_STREAM_FAILED", "建立通知流失败：" + error.getMessage());
        }
        return emitter;
    }

    /** 广播一条"有新通知"事件（只带未读数，不带正文；正文仍需鉴权接口取）。 */
    public void publishUnread(long unread) {
        emitters.forEach((staff, list) -> list.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("notifications").data(Map.of("unread", unread)));
            } catch (IOException error) {
                remove(staff, emitter);
            }
        }));
    }

    private void remove(String staffUsername, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(staffUsername);
        if (list == null) {
            return;
        }
        list.remove(emitter);
        if (list.isEmpty()) {
            emitters.remove(staffUsername);
        }
    }

    private void purgeExpiredTokens() {
        Instant now = Instant.now();
        tokens.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }
}
