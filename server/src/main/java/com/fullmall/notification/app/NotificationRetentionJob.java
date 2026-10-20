package com.fullmall.notification.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 消息保留清理：删除超过保留期的站内消息。
 *
 * <p>**默认关闭**（`app.notification.retention.enabled=false`），由运维显式开启；
 * 保留天数 `app.notification.retention.days`（默认 180）。
 * 关闭状态不会清理任何数据——消息只增不减是有代价的，但是"默认不做破坏性操作"更重要。
 */
@Component
@ConditionalOnProperty(prefix = "app.notification.retention", name = "enabled", havingValue = "true")
public class NotificationRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetentionJob.class);

    private final NotificationService notifications;
    private final int days;

    public NotificationRetentionJob(NotificationService notifications,
                                    @Value("${app.notification.retention.days:180}") int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("消息保留天数必须大于 0，实际=" + days);
        }
        this.notifications = notifications;
        this.days = days;
    }

    @Scheduled(cron = "${app.notification.retention.cron:0 30 4 * * *}")
    public void purge() {
        try {
            int removed = notifications.purgeOlderThan(days);
            if (removed > 0) {
                log.info("消息清理完成：删除 {} 条（保留 {} 天）", removed, days);
            }
        } catch (RuntimeException error) {
            log.error("消息清理失败（本轮跳过，下一轮重试）", error);
        }
    }
}
