package com.fullmall.member.app;

import com.fullmall.platform.schedule.ScheduledJob;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 积分过期任务（cron 后台可改）。
 *
 * <p>**默认开启**：配置为 NEVER 时任务直接返回、不扫描（无副作用）；
 * 但若运营改成 ROLLING_DAYS 后任务没开，积分就永远不会过期——这比空跑更危险。
 */
@Component
public class PointsExpiryTask implements ScheduledJob {

    private final PointsExpiryService service;

    public PointsExpiryTask(PointsExpiryService service) {
        this.service = service;
    }

    @Override
    public String code() {
        return "member.points-expiry";
    }

    @Override
    public String name() {
        return "积分过期处理";
    }

    @Override
    public String defaultCron() {
        return "0 40 3 * * *";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void run() {
        service.expireNow(Instant.now());
    }
}
