package com.fullmall.notification.api;

import com.fullmall.notification.app.NotificationService;
import com.fullmall.notification.domain.Notification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Notification> list(@RequestParam(defaultValue = "100") int limit,
                                   @RequestParam(required = false) String channel,
                                   @RequestParam(required = false) String recipientType) {
        return notificationService.findRecent(limit, channel, recipientType);
    }

    @PostMapping("/{id}/read")
    public void read(@PathVariable UUID id) {
        notificationService.markRead(id);
    }
}