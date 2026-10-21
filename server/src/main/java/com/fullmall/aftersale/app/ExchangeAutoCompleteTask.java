package com.fullmall.aftersale.app;

import com.fullmall.aftersale.infra.AfterSaleRepository;
import com.fullmall.platform.schedule.ScheduledJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 换货到期自动完成。
 *
 * <p>业务口径（用户确认 2026-10-19）：自营场景下，换货发出后超过 N 天，
 * 只要顾客没有申报"未收到"，就视为确认收货并自动完成——大量顾客不会主动点确认，
 * 一直停在 `EXCHANGE_SHIPPED` 会阻塞售后结案与佣金结算；有争议走客服。
 *
 * <p>安全阀：`disputed_at` 一旦写入，该单**永不自动完成**，必须人工处理。
 */
@Component
public class ExchangeAutoCompleteTask implements ScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(ExchangeAutoCompleteTask.class);

    private final AfterSaleRepository repository;
    private final AfterSaleService afterSaleService;
    private final int days;

    public ExchangeAutoCompleteTask(AfterSaleRepository repository,
                                    AfterSaleService afterSaleService,
                                    @Value("${app.after-sale.exchange-auto-complete-days:7}") int days) {
        if (days <= 0 || days > 60) {
            throw new IllegalArgumentException("换货自动完成天数必须在 1..60，实际=" + days);
        }
        this.repository = repository;
        this.afterSaleService = afterSaleService;
        this.days = days;
    }

    public int autoCompleteDays() {
        return days;
    }

    @Override
    public String code() {
        return "after-sale.exchange-auto-complete";
    }

    @Override
    public String name() {
        return "换货到期自动完成";
    }

    @Override
    public String defaultCron() {
        return "0 */10 * * * *";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    @Transactional
    public void run() {
        List<UUID> due = repository.findAutoCompletable(200);
        for (UUID id : due) {
            repository.completeExchangeAuto(id);
            repository.insertEvent(id, "AUTO_COMPLETED",
                    "发货后超过 " + days + " 天且顾客未申报未收到，系统自动完成换货", "系统");
            afterSaleService.closeCommissionHoldForAutoComplete(id);
        }
        if (!due.isEmpty()) {
            log.info("换货自动完成：{} 单", due.size());
        }
    }
}
