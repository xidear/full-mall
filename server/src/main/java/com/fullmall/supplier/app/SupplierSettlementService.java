package com.fullmall.supplier.app;

import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import com.fullmall.supplier.infra.SupplierSettlementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 供应商结算（SUP-020）。
 *
 * <p>当前口径：应付 = 已支付金额 − 退款 − 佣金；**退款聚合尚未接入（按 0）**，佣金费率未配置时为 0。
 * 这两项都在 `SettlementCalculator` 文档与实施记录里标明，不假装已是完整对账。
 */
@Service
public class SupplierSettlementService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(SupplierSettlementService.class);

    private final SupplierSettlementRepository repository;
    private final AuditEventService auditEvents;
    private final BigDecimal commissionRate;
    private final SupplierReturnService returns;
    private final com.fullmall.exchange.app.ExchangeRateService exchangeRates;
    private final String settlementCurrency;

    public SupplierSettlementService(SupplierSettlementRepository repository,
                                     AuditEventService auditEvents,
                                     @Value("${app.supplier.settlement.commission-rate:0}") BigDecimal commissionRate,
                                     SupplierReturnService returns,
                                     com.fullmall.exchange.app.ExchangeRateService exchangeRates,
                                     @Value("${app.fx.base-currency}") String settlementCurrency) {
        this.exchangeRates = exchangeRates;
        this.settlementCurrency = settlementCurrency == null ? null
                : settlementCurrency.trim().toUpperCase(java.util.Locale.ROOT);
        if (this.settlementCurrency == null || this.settlementCurrency.isBlank()) {
            throw new IllegalStateException("app.fx.base-currency 未配置");
        }
        this.repository = repository;
        this.auditEvents = auditEvents;
        this.commissionRate = commissionRate;
        this.returns = returns;
    }

    @Transactional
    public SupplierSettlementRepository.SettlementRow generate(UUID supplierId, Instant periodStart,
                                                               Instant periodEnd, String actor) {
        if (actor == null || actor.isBlank()) {
            throw ApiException.badRequest("ACTOR_REQUIRED", "生成结算单必须记录操作人");
        }
        if (periodStart == null || periodEnd == null || !periodEnd.isAfter(periodStart)) {
            throw ApiException.badRequest("INVALID_SETTLEMENT_PERIOD",
                    "结算周期非法: start=" + periodStart + ", end=" + periodEnd);
        }

        List<SupplierSettlementRepository.PeriodAggregate> aggregates =
                repository.aggregate(supplierId, periodStart, periodEnd);
        if (aggregates.isEmpty()) {
            throw ApiException.conflict("NO_SETTLEABLE_ORDERS",
                    "该周期内没有可结算的已支付供应商代发订单");
        }
        if (aggregates.size() > 1) {
            // 多币种不再拒绝：逐币种按生效汇率折算到结算币种（缺失汇率明确失败）
            log.info("结算周期包含多币种订单，将折算到 {}: {}", settlementCurrency,
                    aggregates.stream().map(SupplierSettlementRepository.PeriodAggregate::currency).toList());
        }

        long grossAmount = 0;
        long returnDeduction = 0;
        int orderCount = 0;
        for (var aggregate : aggregates) {
            grossAmount += toSettlement(aggregate.grossAmount(), aggregate.currency(), periodEnd);
            long returnsInCurrency = returns.settledAmount(supplierId, periodStart, periodEnd,
                    aggregate.currency());
            returnDeduction += toSettlement(returnsInCurrency, aggregate.currency(), periodEnd);
            orderCount += aggregate.orderCount();
        }
        var amounts = SettlementCalculator.calculate(grossAmount, returnDeduction, commissionRate);

        UUID id = UUID.randomUUID();
        repository.insert(id, supplierId, periodStart, periodEnd, orderCount,
                amounts.grossAmount(), amounts.refundAmount(), amounts.commissionAmount(),
                amounts.payableAmount(), settlementCurrency, "DRAFT");

        auditEvents.record(new AuditEventService.AuditEvent(
                "SETTLEMENT_GENERATED", actor, "supplier_settlement", id.toString(),
                "supplier=" + supplierId + ";payable=" + amounts.payableAmount()
                        + ";currency=" + settlementCurrency, null));

        return repository.findBySupplier(supplierId, 1).stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("结算单写入后读取失败: " + id));
    }

    public List<SupplierSettlementRepository.SettlementRow> list(UUID supplierId, int limit) {
        if (limit <= 0 || limit > 200) {
            throw ApiException.badRequest("INVALID_LIMIT", "查询条数必须在 1..200，实际=" + limit);
        }
        return repository.findBySupplier(supplierId, limit);
    }

    /** 折算到结算币种；缺汇率明确失败，不按 1:1 兜底。 */
    private long toSettlement(long amount, String fromCurrency, Instant at) {
        try {
            return exchangeRates.convert(amount, fromCurrency, settlementCurrency, at)
                    .convertedAmount();
        } catch (ApiException failure) {
            if (!"EXCHANGE_RATE_NOT_FOUND".equals(failure.code())) {
                throw failure;
            }
            throw ApiException.conflict("SETTLEMENT_FX_RATE_MISSING",
                    "结算缺少汇率 " + fromCurrency + " -> " + settlementCurrency
                            + "（生效时间 " + at + "），请先在「汇率」页维护后再生成结算单");
        }
    }
}