package com.fullmall.notification.api;

import com.fullmall.notification.app.NotificationService;
import com.fullmall.notification.domain.Notification;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * 通知后台运维接口。
 *
 * <p>独立于会员侧控制器：会员端前缀是 `/api/v1/notifications`，
 * 若把 admin 路径写在同一个类里会被拼成 `/api/v1/notifications/admin/...`（实际踩过，404）。
 */
@RestController
@RequestMapping("/admin/v1/notifications")
public class NotificationAdminController {

    private final NotificationService notifications;

    public NotificationAdminController(NotificationService notifications) {
        this.notifications = notifications;
    }

    public record TestSend(String channel, String title, String content) {
    }

    /**
     * 后台未读数（右上角铃铛）。
     *
     * <p>只统计 STAFF 收件人的未读：把顾客通知也算进来，运营永远清不完，徽标就失去意义。
     * 具体条数与内容走 {@code GET /admin/v1/notifications}（已有列表接口）。
     */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication authentication) {
        // 按人算：广播通知 A 读了不影响 B
        return Map.of("unread", notifications.unreadCountForStaff(authentication.getName()));
    }

    /** 渠道连通性自检：发一条测试消息并返回真实投递结果（不伪造成功）。 */
    @PostMapping("/test-send")
    public Notification testSend(@RequestBody TestSend body) {
        return notifications.sendTest(body.channel(), body.title(), body.content());
    }

    /** 重试失败投递：仅 FAILED 可重试（已 SENT 的重试会重复打扰顾客）。 */
    @PostMapping("/{id}/retry")
    public Notification retry(@PathVariable UUID id) {
        return notifications.retry(id);
    }
}
