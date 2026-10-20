package com.fullmall.notification.app;

import com.fullmall.platform.schedule.ScheduledJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 消息保留清理任务（图形化配置 cron）。
 *
 * <p>原先是 `@Scheduled` + 属性开关，改 cron 必须重启；现改为注册表驱动，
 * cron 与启停由后台控制，默认**关闭**。
 */
@Component
public class NotificationRetentionTask implements ScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetentionTask.class);

    private final NotificationService notifications;
    private final int days;

    public NotificationRetentionTask(NotificationService notifications,
                                     @Value("${app.notification.retention.days:180}") int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("消息保留天数必须大于 0，实际=" + days);
        }
        this.notifications = notifications;
        this.days = days;
    }

    @Override
    public String code() {
        return "notification.retention";
    }

    @Override
    public String name() {
        return "站内消息清理（保留 " + days + " 天）";
    }

    @Override
    public String defaultCron() {
        return "0 30 4 * * *";
    }

    @Override
    public boolean enabledByDefault() {
        return false;
    }

    @Override
    public void run() {
        int removed = notifications.purgeOlderThan(days);
        if (removed > 0) {
            log.info("消息清理完成：删除 {} 条（保留 {} 天）", removed, days);
        }
    }
}
