package com.fullmall.distribution.app;

import com.fullmall.platform.schedule.ScheduledJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** 佣金解冻任务（图形化配置 cron，默认关闭）。 */
@Component
public class CommissionReleaseTask implements ScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(CommissionReleaseTask.class);

    private final CommissionLedgerService ledger;
    private final DistributionConfig distributionConfig;

    public CommissionReleaseTask(CommissionLedgerService ledger, DistributionConfig distributionConfig) {
        this.ledger = ledger;
        this.distributionConfig = distributionConfig;
    }

    @Override
    public String code() {
        return "distribution.commission-release";
    }

    @Override
    public String name() {
        return "佣金解冻（冻结期到期转可提现）";
    }

    @Override
    public String defaultCron() {
        return "0 */5 * * * *";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void run() {
        var config = distributionConfig.load();
        Instant now = Instant.now();
        // 两种来源都要处理：显式 frozen_until（FIXED_DAYS 或历史数据）与
        // 售后关闭策略（frozen_until 为空、订单完成且过窗口）。只跑一种会漏解冻。
        int released = ledger.releaseDue(now);
        if ("AFTER_SALE_CLOSED".equals(config.freezePolicy())) {
            released += ledger.releaseAfterSaleClosed(config.freezeDays(), now);
        }
        if (released > 0) {
            log.info("佣金解冻完成，共 {} 条（策略 {}）", released, config.freezePolicy());
        }
    }
}
