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

    private final SupplierSettlementRepository repository;
    private final AuditEventService auditEvents;
    private final BigDecimal commissionRate;

    public SupplierSettlementService(SupplierSettlementRepository repository,
                                     AuditEventService auditEvents,
                                     @Value("${app.supplier.settlement.commission-rate:0}") BigDecimal commissionRate) {
        this.repository = repository;
        this.auditEvents = auditEvents;
        this.commissionRate = commissionRate;
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
            throw ApiException.conflict("MULTI_CURRENCY_PERIOD",
                    "该周期包含多币种订单，暂不支持自动换算，请按币种分批结算: "
                            + aggregates.stream().map(SupplierSettlementRepository.PeriodAggregate::currency).toList());
        }

        var aggregate = aggregates.get(0);
        // 退款暂按 0；待售后/退货对账接入后替换该入参，公式不变。
        var amounts = SettlementCalculator.calculate(aggregate.grossAmount(), 0L, commissionRate);

        UUID id = UUID.randomUUID();
        repository.insert(id, supplierId, periodStart, periodEnd, aggregate.orderCount(),
                amounts.grossAmount(), amounts.refundAmount(), amounts.commissionAmount(),
                amounts.payableAmount(), aggregate.currency(), "DRAFT");

        auditEvents.record(new AuditEventService.AuditEvent(
                "SETTLEMENT_GENERATED", actor, "supplier_settlement", id.toString(),
                "supplier=" + supplierId + ";payable=" + amounts.payableAmount()
                        + ";currency=" + aggregate.currency(), null));

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
}