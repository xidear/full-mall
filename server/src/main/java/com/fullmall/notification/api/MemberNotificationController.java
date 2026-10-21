package com.fullmall.notification.api;

import com.fullmall.notification.app.NotificationService;
import com.fullmall.notification.domain.Notification;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 顾客侧消息中心：列表 / 未读数 / 标记已读。 */
@RestController
@RequestMapping("/api/v1/notifications")
public class MemberNotificationController {

    private static final String RECIPIENT_TYPE = "MEMBER";

    private final NotificationService notifications;

    public MemberNotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    private String member(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw com.fullmall.platform.error.ApiException.badRequest("MEMBER_REQUIRED", "需要会员登录");
        }
        return authentication.getName();
    }

    @GetMapping
    public List<Notification> list(@RequestParam(defaultValue = "20") int limit,
                                   Authentication authentication) {
        return notifications.findForRecipient(RECIPIENT_TYPE, member(authentication), limit);
    }

    @GetMapping("/unread-count")
    public Map<String, Integer> unread(Authentication authentication) {
        return Map.of("unread", notifications.unreadCount(RECIPIENT_TYPE, member(authentication)));
    }

    public record TestSend(String channel, String title, String content) {
    }

    /** 后台渠道连通性自检：发一条测试消息并返回投递结果（不伪造成功）。 */
    @PostMapping("/admin/v1/notifications/test-send")
    public com.fullmall.notification.domain.Notification testSend(@RequestBody TestSend body) {
        return notifications.sendTest(body.channel(), body.title(), body.content());
    }

    /** 后台重试失败通知（运营排障用）。路由放在 admin 命名空间由控制器注解决定。 */
    @PostMapping("/admin/v1/notifications/{id}/retry")
    public com.fullmall.notification.domain.Notification retry(@PathVariable UUID id) {
        return notifications.retry(id);
    }

    @PostMapping("/{id}/read")
    public Map<String, Boolean> markRead(@PathVariable UUID id, Authentication authentication) {
        return Map.of("read", notifications.markRead(RECIPIENT_TYPE, member(authentication), id));
    }
}
