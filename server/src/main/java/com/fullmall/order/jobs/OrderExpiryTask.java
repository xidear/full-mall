package com.fullmall.order.jobs;

import com.fullmall.platform.schedule.ScheduledJob;
import org.springframework.stereotype.Component;

/**
 * 订单超时任务（图形化配置 cron，默认关闭）。
 *
 * <p>**时长参数不进 cron**：`app.reservation.expiry-minutes` 仍由配置控制，
 * cron 只决定"多久扫一次"。两者概念不同，混在一起会让运维误以为改 cron 就能改超时时长。
 */
@Component
public class OrderExpiryTask implements ScheduledJob {

    private final OrderExpiryJob job;

    public OrderExpiryTask(OrderExpiryJob job) {
        this.job = job;
    }

    @Override
    public String code() {
        return "order.expiry";
    }

    @Override
    public String name() {
        return "超时未支付订单取消（扫描间隔由 cron 控制）";
    }

    @Override
    public String defaultCron() {
        return "0 */1 * * * *";
    }

    @Override
    public boolean enabledByDefault() {
        return false;
    }

    @Override
    public void run() {
        job.cancelExpiredOrders();
    }
}
