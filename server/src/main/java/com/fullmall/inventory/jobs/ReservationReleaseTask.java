package com.fullmall.inventory.jobs;

import com.fullmall.platform.schedule.ScheduledJob;
import org.springframework.stereotype.Component;

/** 库存预留释放任务（图形化配置 cron，默认关闭）。 */
@Component
public class ReservationReleaseTask implements ScheduledJob {

    private final ReservationExpiryJob job;

    public ReservationReleaseTask(ReservationExpiryJob job) {
        this.job = job;
    }

    @Override
    public String code() {
        return "inventory.reservation-release";
    }

    @Override
    public String name() {
        return "过期库存预留释放";
    }

    @Override
    public String defaultCron() {
        return "0 */1 * * * *";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void run() {
        job.releaseExpiredReservations();
    }
}
