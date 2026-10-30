package com.fullmall.notification.api;

import com.fullmall.notification.app.NotificationStreamService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 通知的实时通道。
 *
 * <p>拆成两个路径是有意的：
 * <ul>
 *   <li>{@code POST /admin/v1/notifications/stream-token} 走正常 Bearer 鉴权（过滤器覆盖 /admin/**）；</li>
 *   <li>{@code GET /api/v1/notifications/stream?token=} 是 EventSource 唯一能用的形态——
 *       它发不出 Authorization 头，所以凭一次性短期 token 通过。流里只推"未读数变化"，
 *       不推正文；正文仍要走鉴权接口。</li>
 * </ul>
 */
@RestController
public class NotificationStreamController {

    private final NotificationStreamService streams;

    public NotificationStreamController(NotificationStreamService streams) {
        this.streams = streams;
    }

    @PostMapping("/admin/v1/notifications/stream-token")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, Object> token(Authentication authentication) {
        return streams.issueToken(authentication.getName());
    }

    @GetMapping("/api/v1/notifications/stream")
    public SseEmitter stream(@RequestParam(required = false) String token) {
        return streams.subscribe(token);
    }
}
