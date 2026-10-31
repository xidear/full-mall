package com.fullmall.notification.api;

import com.fullmall.notification.app.NotificationService;
import com.fullmall.notification.domain.Notification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 当前员工可见的通知：本人的 + 广播（recipient_id='ALL'）。
     *
     * <p>不再暴露 CUSTOMER/MEMBER 通知——它们的 receiver 是顾客手机号，属于 PII。
     * 需要看某个顾客的通知请在「客户详情」上下文里查（后续实现），并单独授权 + 脱敏。
     */
    @GetMapping
    public List<Notification> list(@RequestParam(defaultValue = "100") int limit,
                                   @RequestParam(required = false) String channel,
                                   org.springframework.security.core.Authentication authentication) {
        return notificationService.findForStaff(authentication.getName(), limit, channel);
    }

    @PostMapping("/{id}/read")
    public void read(@PathVariable UUID id, org.springframework.security.core.Authentication authentication) {
        notificationService.markReadForStaff(authentication.getName(), id);
    }

    /** 认领：OPEN 变 ACK。已读不等于已处理。 */
    @PostMapping("/{id}/ack")
    public Map<String, Object> ack(@PathVariable UUID id, Authentication authentication) {
        return Map.of("changed", notificationService.ackForStaff(authentication.getName(), id));
    }

    /** 修复完成：OPEN/ACK 变 RESOLVED。 */
    @PostMapping("/{id}/resolve")
    public Map<String, Object> resolve(@PathVariable UUID id, Authentication authentication) {
        return Map.of("changed", notificationService.resolveForStaff(authentication.getName(), id));
    }

    /** 全部已读：只影响自己（广播按人记已读）。 */
    @PostMapping("/read-all")
    public Map<String, Integer> readAll(org.springframework.security.core.Authentication authentication) {
        return Map.of("marked", notificationService.markAllReadForStaff(authentication.getName()));
    }
}