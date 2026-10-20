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

    public CommissionReleaseTask(CommissionLedgerService ledger) {
        this.ledger = ledger;
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
        return false;
    }

    @Override
    public void run() {
        int released = ledger.releaseDue(Instant.now());
        if (released > 0) {
            log.info("佣金解冻完成，共 {} 条", released);
        }
    }
}
