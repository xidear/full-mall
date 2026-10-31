package com.fullmall.exchange.api;

import com.fullmall.exchange.app.ExchangeRateFetchService;
import com.fullmall.exchange.app.ExchangeRateService;
import com.fullmall.platform.audit.AuditEventService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 汇率管理（阶段 1）。
 *
 * <p>权限走 {@code StaffAuthFilter} 的路径映射（/admin/v1/exchange-rates → settings.manage）。
 * 这里只提供“维护 + 换算”，**不触发任何改价**：订单金额仍按市场定价，历史订单不回算。
 */
@RestController
@RequestMapping("/admin/v1/exchange-rates")
public class AdminExchangeRateController {

    private final ExchangeRateService service;
    private final ExchangeRateFetchService fetcher;
    private final AuditEventService auditEvents;

    public AdminExchangeRateController(ExchangeRateService service, ExchangeRateFetchService fetcher,
                                       AuditEventService auditEvents) {
        this.service = service;
        this.fetcher = fetcher;
        this.auditEvents = auditEvents;
    }

    public record CreateRate(String baseCurrency, String quoteCurrency, BigDecimal rate,
                             String source, Instant effectiveAt) {
    }

    @GetMapping
    public List<ExchangeRateService.RateView> list(@RequestParam(defaultValue = "100") int limit) {
        return service.list(limit);
    }

    /** 自动汇率状态：未配置/演示 mock/上次成功/失败原因。 */
    @GetMapping("/provider")
    public ExchangeRateFetchService.Status provider() {
        return fetcher.status();
    }

    /**
     * 立即拉取一次。失败不返回 5xx：状态里带 lastError，已有汇率保持不变。
     */
    @PostMapping("/refresh")
    public ExchangeRateFetchService.Status refresh(Authentication authentication) {
        return fetcher.refresh(authentication.getName());
    }

    @PutMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeRateService.RateView create(@RequestBody CreateRate body,
                                               Authentication authentication) {
        ExchangeRateService.RateView created = service.upsert(body.baseCurrency(),
                body.quoteCurrency(), body.rate(), body.source(), body.effectiveAt(),
                authentication.getName());
        auditEvents.record(new AuditEventService.AuditEvent("EXCHANGE_RATE_CREATED",
                authentication.getName(), "exchange_rate", created.id().toString(),
                created.baseCurrency() + "->" + created.quoteCurrency() + " rate=" + created.rate(),
                null));
        return created;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        service.delete(id, authentication.getName());
        auditEvents.record(new AuditEventService.AuditEvent("EXCHANGE_RATE_DELETED",
                authentication.getName(), "exchange_rate", id.toString(), null, null));
    }

    /**
     * 精确换算（最小单位进、最小单位出）。amount 必须是非负整数；
     * 不做"找不到就按 1:1"的兜底。
     */
    @GetMapping("/convert")
    public ExchangeRateService.Conversion convert(@RequestParam long amount,
                                                  @RequestParam String from,
                                                  @RequestParam String to,
                                                  @RequestParam(required = false) Instant at) {
        return service.convert(amount, from, to, at);
    }

    /** 对账/客服常用：返回主单位文本，避免调用方自己除 10^n。 */
    @GetMapping("/convert-text")
    public String convertText(@RequestParam long amount, @RequestParam String from,
                              @RequestParam String to,
                              @RequestParam(required = false) Instant at) {
        ExchangeRateService.Conversion conversion = service.convert(amount, from, to, at);
        return com.fullmall.platform.money.Money.toMajor(
                conversion.convertedAmount(), conversion.toCurrency());
    }

    /** POST 版本便于前端把较长的金额/时间放在 body；语义与 GET 一致。 */
    @PostMapping("/convert")
    public ExchangeRateService.Conversion convertPost(@RequestBody ConvertRequest body) {
        return service.convert(body.amount(), body.from(), body.to(), body.at());
    }

    public record ConvertRequest(long amount, String from, String to, Instant at) {
    }
}