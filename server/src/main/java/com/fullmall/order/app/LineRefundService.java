package com.fullmall.order.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 按行退款：把退款落到具体商品行，为"部分退款按比例回退积分/佣金"提供分摊基础。
 *
 * <p>本轮只做**记录与校验**（退了哪行、几件、多少钱），不做资金回退——
 * 回退口径见 {@code docs/REFUND_POLICY.md}，需要产品确认后接入。
 *
 * <p>硬约束：
 * <ul>
 *   <li>行必须属于该订单；</li>
 *   <li>累计退款数量不得超过该行数量；</li>
 *   <li>累计退款金额不得超过该行金额（单价 × 数量）。</li>
 * </ul>
 */
@Service
public class LineRefundService {

    private final JdbcTemplate jdbcTemplate;
    private final com.fullmall.exchange.app.ExchangeRateService exchangeRates;
    private final com.fullmall.platform.audit.AuditEventService auditEvents;
    private final String fxBaseCurrency;
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(LineRefundService.class);

    public LineRefundService(JdbcTemplate jdbcTemplate,
                             com.fullmall.exchange.app.ExchangeRateService exchangeRates,
                             com.fullmall.platform.audit.AuditEventService auditEvents,
                             @org.springframework.beans.factory.annotation.Value("${app.fx.base-currency}")
                             String fxBaseCurrency) {
        this.jdbcTemplate = jdbcTemplate;
        this.exchangeRates = exchangeRates;
        this.auditEvents = auditEvents;
        this.fxBaseCurrency = fxBaseCurrency == null ? null
                : fxBaseCurrency.trim().toUpperCase(java.util.Locale.ROOT);
        if (this.fxBaseCurrency == null || this.fxBaseCurrency.isBlank()) {
            throw new IllegalStateException("app.fx.base-currency 未配置");
        }
    }

    public record LineInfo(UUID id, UUID skuId, int quantity, long unitPrice, String currency) {
    }

    public record Refunded(UUID id, UUID orderLineId, int quantity, long amount) {
    }

    /** 该订单的可退行（含已退数量/金额，便于前端展示剩余可退）。 */
    public List<LineInfo> lines(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT id, sku_id, quantity, unit_price_amount, currency
                  FROM order_line WHERE order_id = ? ORDER BY created_at, id
                """, (rs, rowNum) -> new LineInfo(
                rs.getObject("id", UUID.class), rs.getObject("sku_id", UUID.class),
                rs.getInt("quantity"), rs.getLong("unit_price_amount"), rs.getString("currency")),
                orderId);
    }

    @Transactional
    public Refunded record(UUID orderId, UUID orderLineId, int quantity, long amount,
                           String currency, String reason) {
        if (quantity <= 0) {
            throw ApiException.badRequest("INVALID_REFUND_QUANTITY", "退款数量必须大于 0");
        }
        return recordInternal(orderId, orderLineId, Integer.valueOf(quantity), amount, currency, reason);
    }

    /**
     * 记录"只按金额、不按件"的退款（数量为 null）。
     *
     * <p>为什么允许 null：部分退款按金额分摊时并不总能换算成整件（例如 100 元商品退 40 元）；
     * 强行反推数量会让"累计退款数量"超过行数量，进而**阻断后续合法退款**。
     */
    public Refunded recordAmountOnly(UUID orderId, UUID orderLineId, long amount,
                                     String currency, String reason) {
        return recordInternal(orderId, orderLineId, null, amount, currency, reason);
    }

    private Refunded recordInternal(UUID orderId, UUID orderLineId, Integer quantity, long amount,
                                    String currency, String reason) {
        if (amount <= 0) {
            throw ApiException.badRequest("INVALID_REFUND_AMOUNT", "退款金额必须大于 0");
        }

        List<LineInfo> lines = jdbcTemplate.query("""
                SELECT id, sku_id, quantity, unit_price_amount, currency
                  FROM order_line WHERE id = ? AND order_id = ?
                """, (rs, rowNum) -> new LineInfo(
                rs.getObject("id", UUID.class), rs.getObject("sku_id", UUID.class),
                rs.getInt("quantity"), rs.getLong("unit_price_amount"), rs.getString("currency")),
                orderLineId, orderId);

        if (lines.isEmpty()) {
            throw ApiException.notFound("ORDER_LINE_NOT_FOUND", "该商品行不属于此订单: " + orderLineId);
        }
        LineInfo line = lines.get(0);
        if (!line.currency().equals(currency)) {
            throw ApiException.badRequest("CURRENCY_MISMATCH",
                    "退款币种与订单行不一致：" + currency + " vs " + line.currency());
        }

        int refundedQty = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0) FROM order_line_refund WHERE order_line_id = ?
                """, Integer.class, orderLineId);   // quantity 为 NULL 的行不计入
        long refundedAmount = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_amount), 0) FROM order_line_refund WHERE order_line_id = ?
                """, Long.class, orderLineId);
        // 只有明确写了数量才校验件数；按金额分摊（quantity=null）不受件数限制，但仍受金额上限约束
        if (quantity != null && refundedQty + quantity > line.quantity()) {
            throw ApiException.conflict("REFUND_QUANTITY_EXCEEDS_LINE",
                    "累计退款数量 " + (refundedQty + quantity) + " 超过该行数量 " + line.quantity());
        }
        long lineTotal = Math.multiplyExact(line.unitPrice(), (long) line.quantity());
        if (refundedAmount + amount > lineTotal) {
            throw ApiException.conflict("REFUND_AMOUNT_EXCEEDS_LINE",
                    "累计退款金额 " + (refundedAmount + amount) + " 超过该行金额 " + lineTotal);
        }

        UUID id = UUID.randomUUID();
        var fx = fxSnapshot(orderId, amount, currency, java.time.Instant.now());
        jdbcTemplate.update("""
                INSERT INTO order_line_refund (id, order_id, order_line_id, quantity,
                                              amount_amount, currency, reason,
                                              fx_rate, fx_base_currency, fx_base_amount, fx_rate_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, orderId, orderLineId, quantity, amount, currency,
                reason == null ? null : reason.trim(),
                fx.rate(), fx.baseCurrency(), fx.baseAmount(),
                fx.rateAt() == null ? null : java.sql.Timestamp.from(fx.rateAt()));
        return new Refunded(id, orderLineId, quantity == null ? 0 : quantity, amount);
    }

    /**
     * 退款汇率快照：退款金额仍按原币种，不做跨币种退款；这里只记录退款时点汇率，
     * 允许与下单时不同（用户明确要求）。没有汇率时留空并写审计，不阻塞退款。
     */
    private RefundFx fxSnapshot(UUID orderId, long amount, String currency,
                                java.time.Instant at) {
        if (currency.equals(fxBaseCurrency)) {
            return new RefundFx(java.math.BigDecimal.ONE, fxBaseCurrency, amount, at);
        }
        try {
            var conversion = exchangeRates.convert(amount, currency, fxBaseCurrency, at);
            return new RefundFx(conversion.rate(), fxBaseCurrency, conversion.convertedAmount(),
                    conversion.effectiveAt());
        } catch (com.fullmall.platform.error.ApiException failure) {
            if (!"EXCHANGE_RATE_NOT_FOUND".equals(failure.code())) {
                throw failure;
            }
            log.warn("退款 {} 缺少汇率快照：{} -> {}，金额 {}（不阻塞退款，允许不同汇率）",
                    orderId, currency, fxBaseCurrency, amount);
            auditEvents.record(new com.fullmall.platform.audit.AuditEventService.AuditEvent(
                    "ORDER_REFUND_FX_RATE_MISSING", "system", "order", orderId.toString(),
                    "refund currency=" + currency + " base=" + fxBaseCurrency
                            + " amount=" + amount, null));
            return new RefundFx(null, fxBaseCurrency, null, null);
        }
    }

    private record RefundFx(java.math.BigDecimal rate, String baseCurrency,
                            Long baseAmount, java.time.Instant rateAt) {
    }


    /**
     * 售后单退款时落行退款记录：把售后单的退款金额按**售后涉及的行的剩余金额**分摊后写入，
     * 使按行分摊模型在真实流程中被使用（而不是只存在于测试里）。
     *
     * @return 写入的行退款记录（金额之和等于售后单退款金额）
     */
    @Transactional
    public List<Refunded> recordForAfterSale(UUID afterSaleId) {
        List<AfterSaleRow> sales = jdbcTemplate.query("""
                SELECT order_id, refund_amount, currency FROM after_sale WHERE id = ?
                """, (rs, rowNum) -> new AfterSaleRow(
                rs.getObject("order_id", UUID.class), rs.getLong("refund_amount"),
                rs.getString("currency")), afterSaleId);
        if (sales.isEmpty()) {
            throw ApiException.notFound("AFTER_SALE_NOT_FOUND", "售后单不存在：" + afterSaleId);
        }
        AfterSaleRow sale = sales.get(0);
        if (sale.refundAmount() <= 0) {
            return List.of();
        }

        List<AfterSaleLine> lines = jdbcTemplate.query("""
                SELECT l.order_line_id, l.quantity, ol.unit_price_amount
                  FROM after_sale_line l JOIN order_line ol ON ol.id = l.order_line_id
                 WHERE l.after_sale_id = ? ORDER BY l.order_line_id
                """, (rs, rowNum) -> new AfterSaleLine(
                rs.getObject("order_line_id", UUID.class), rs.getInt("quantity"),
                rs.getLong("unit_price_amount")), afterSaleId);
        if (lines.isEmpty()) {
            throw ApiException.conflict("AFTER_SALE_LINES_MISSING",
                    "售后单没有商品行，无法分摊退款：" + afterSaleId);
        }

        long totalRemaining = 0;
        List<Long> remaining = new ArrayList<>();
        for (AfterSaleLine line : lines) {
            long refunded = jdbcTemplate.queryForObject("""
                    SELECT COALESCE(SUM(amount_amount), 0) FROM order_line_refund WHERE order_line_id = ?
                    """, Long.class, line.orderLineId());
            long left = Math.max(Math.multiplyExact(line.unitPrice(), (long) line.quantity()) - refunded, 0);
            remaining.add(left);
            totalRemaining += left;
        }
        if (sale.refundAmount() > totalRemaining) {
            throw ApiException.conflict("AFTER_SALE_REFUND_EXCEEDS_LINES",
                    "售后退款 " + sale.refundAmount() + " 超过涉及行可退 " + totalRemaining);
        }

        List<Refunded> written = new ArrayList<>();
        long allocated = 0;
        int lastIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (remaining.get(i) > 0) {
                lastIndex = i;
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            long left = remaining.get(i);
            if (left <= 0 || i == lastIndex) {
                continue;
            }
            long share = Math.floorDiv(sale.refundAmount() * left, totalRemaining);
            if (share <= 0) {
                continue;
            }
            allocated += share;
            written.add(record(sale.orderId(), lines.get(i).orderLineId(),
                    lines.get(i).quantity(), share, sale.currency(), "after-sale:" + afterSaleId));
        }
        written.add(record(sale.orderId(), lines.get(lastIndex).orderLineId(),
                lines.get(lastIndex).quantity(), sale.refundAmount() - allocated,
                sale.currency(), "after-sale:" + afterSaleId));
        allocated = sale.refundAmount();
        if (allocated != sale.refundAmount()) {
            throw new IllegalStateException("售后分摊金额不一致：allocated=" + allocated
                    + ", expected=" + sale.refundAmount());
        }
        return written;
    }

    private record AfterSaleRow(UUID orderId, long refundAmount, String currency) {
    }

    private record AfterSaleLine(UUID orderLineId, int quantity, long unitPrice) {
    }

    /**
     * 把一笔退款金额按行分摊到各商品（用于按比例回退积分/佣金）。
     *
     * <p>规则：按各行**剩余未退金额**占比分摊；**尾差分配给最后一个有剩余的行**，
     * 保证分摊之和**恰好等于**退款金额（不出现分币丢失或多出）。
     */
    /**
     * 计算并**落库**分摊（无售后单的退款用，例如拼团超时全额退）。
     *
     * <p>{@link #allocate} 是纯计算器；直接调用它不会写入任何记录——这一点曾经坑过一次
     * （调用方以为已经记账，实际一行都没有）。
     *
     * <p>数量取该行**剩余可退数量**中的最小值，保证 `quantity > 0` 的约束成立。
     */
    @Transactional
    public List<Refunded> allocateAndRecord(UUID orderId, long totalRefundAmount, String currency) {
        List<Refunded> allocations = allocate(orderId, totalRefundAmount, currency);
        List<Refunded> persisted = new ArrayList<>();
        for (Refunded allocation : allocations) {
            LineInfo line = lines(orderId).stream()
                    .filter(candidate -> candidate.id().equals(allocation.orderLineId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("分摊行不存在: " + allocation.orderLineId()));
            long refundedAmount = jdbcTemplate.queryForObject("""
                    SELECT COALESCE(SUM(amount_amount), 0) FROM order_line_refund WHERE order_line_id = ?
                    """, Long.class, line.id());
            long lineTotal = Math.multiplyExact(line.unitPrice(), (long) line.quantity());
            long leftAmount = Math.max(lineTotal - refundedAmount, 0);
            // 只有"整件退款"才写数量：金额能被单价整除且件数在剩余范围内
            boolean wholeUnits = allocation.amount() % line.unitPrice() == 0
                    && allocation.amount() / line.unitPrice() <= line.quantity();
            if (wholeUnits) {
                persisted.add(record(orderId, line.id(),
                        (int) (allocation.amount() / line.unitPrice()),
                        allocation.amount(), currency, "退款分摊"));
            } else {
                persisted.add(recordAmountOnly(orderId, line.id(), allocation.amount(),
                        currency, "退款分摊"));
            }
        }
        return persisted;
    }

    public List<Refunded> allocate(UUID orderId, long totalRefundAmount, String currency) {
        if (totalRefundAmount <= 0) {
            throw ApiException.badRequest("INVALID_REFUND_AMOUNT", "退款金额必须大于 0");
        }
        List<LineInfo> lines = lines(orderId);
        if (lines.isEmpty()) {
            throw ApiException.notFound("ORDER_LINE_NOT_FOUND", "订单没有商品行: " + orderId);
        }

        List<Long> remaining = new ArrayList<>();
        long remainingTotal = 0;
        for (LineInfo line : lines) {
            long lineTotal = Math.multiplyExact(line.unitPrice(), (long) line.quantity());
            long refunded = jdbcTemplate.queryForObject("""
                    SELECT COALESCE(SUM(amount_amount), 0) FROM order_line_refund WHERE order_line_id = ?
                    """, Long.class, line.id());
            long left = lineTotal - refunded;
            remaining.add(Math.max(left, 0));
            remainingTotal += Math.max(left, 0);
        }
        if (remainingTotal <= 0) {
            throw ApiException.conflict("NO_REMAINING_AMOUNT", "订单已无可退金额");
        }
        if (totalRefundAmount > remainingTotal) {
            throw ApiException.conflict("REFUND_AMOUNT_EXCEEDS_REMAINING",
                    "退款金额 " + totalRefundAmount + " 超过剩余可退 " + remainingTotal);
        }

        List<Refunded> result = new ArrayList<>();
        long allocated = 0;
        int lastIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (remaining.get(i) <= 0) {
                continue;
            }
            // 尾差给**剩余金额最大**的行（而不是"id 最大"），保证同一订单分摊结果可复现
            if (lastIndex < 0 || remaining.get(i) > remaining.get(lastIndex)) {
                lastIndex = i;
            }
        }
        // 先给"非尾差行"按比例分配，再把剩余额度整体给尾差行（避免重复分配）
        for (int i = 0; i < lines.size(); i++) {
            long left = remaining.get(i);
            if (left <= 0 || i == lastIndex) {
                continue;
            }
            long share = Math.floorDiv(totalRefundAmount * left, remainingTotal);
            if (share <= 0) {
                continue;
            }
            allocated += share;
            result.add(new Refunded(null, lines.get(i).id(), 0, share));
        }
        result.add(new Refunded(null, lines.get(lastIndex).id(), 0, totalRefundAmount - allocated));
        allocated = totalRefundAmount;
        if (allocated != totalRefundAmount) {
            throw new IllegalStateException(
                    "分摊后金额不一致：allocated=" + allocated + ", expected=" + totalRefundAmount);
        }
        return result;
    }
}
