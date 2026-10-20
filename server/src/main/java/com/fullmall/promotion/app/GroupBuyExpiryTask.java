package com.fullmall.promotion.app;

import com.fullmall.platform.schedule.ScheduledJob;
import org.springframework.stereotype.Component;

/**
 * 拼团过期任务（图形化配置 cron，默认关闭）。
 *
 * <p>逻辑仍在 `PromotionService.expireGroupBuys`（会发布失败事件触发退款），
 * 本类只负责把它接入注册表——避免把业务逻辑复制一份到调度层。
 */
@Component
public class GroupBuyExpiryTask implements ScheduledJob {

    private final PromotionService promotions;

    public GroupBuyExpiryTask(PromotionService promotions) {
        this.promotions = promotions;
    }

    @Override
    public String code() {
        return "promotion.group-buy-expiry";
    }

    @Override
    public String name() {
        return "拼团过期处理（失败并触发退款）";
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
        promotions.expireGroupBuys();
    }
}
