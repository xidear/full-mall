package com.fullmall.member.app;

import com.fullmall.platform.schedule.ScheduledJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * 生日礼积分定时任务（默认每天 00:10 执行，cron 可在后台改）。
 *
 * <p>为什么默认开启：不发只是少发福利，不会造成资金/库存事故；但漏发会被会员投诉。
 */
@Component
public class BirthdayGiftTask implements ScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(BirthdayGiftTask.class);

    private final BirthdayGiftService service;

    public BirthdayGiftTask(BirthdayGiftService service) {
        this.service = service;
    }

    @Override
    public String code() {
        return "member.birthday-gift";
    }

    @Override
    public String name() {
        return "会员生日礼积分";
    }

    @Override
    public String defaultCron() {
        return "0 10 0 * * *";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void run() {
        var granted = service.grantToday();
        if (!granted.isEmpty()) {
            log.info("生日礼积分：本次发放 {} 人", granted.size());
        }
    }
}
