package com.fullmall.aftersale.app;

import com.fullmall.aftersale.api.AfterSaleDtos.ApproveAfterSaleRequest;
import com.fullmall.aftersale.api.AfterSaleDtos.AfterSaleView;
import com.fullmall.aftersale.api.AfterSaleDtos.CreateAfterSaleRequest;
import com.fullmall.aftersale.api.AfterSaleDtos.ReceiveAfterSaleRequest;
import com.fullmall.aftersale.api.AfterSaleDtos.RejectAfterSaleRequest;
import com.fullmall.aftersale.infra.AfterSaleRepository;
import com.fullmall.inventory.domain.LocationType;
import com.fullmall.order.app.OrderService;
import com.fullmall.order.domain.Order;
import com.fullmall.order.domain.OrderLine;
import com.fullmall.order.domain.OrderStatus;
import com.fullmall.payment.app.PaymentService;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.util.Ids;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AfterSaleService {

    private final AfterSaleRepository repository;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final com.fullmall.order.app.LineRefundService lineRefunds;
    private final com.fullmall.distribution.app.CommissionHoldService commissionHold;
    @org.springframework.beans.factory.annotation.Value("${app.after-sale.exchange-auto-complete-days:7}")
    private int exchangeAutoCompleteDays;
    private final com.fullmall.notification.app.NotificationService notifications;
    private final com.fullmall.inventory.app.InventoryService inventoryService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public AfterSaleService(AfterSaleRepository repository, OrderService orderService,
                            PaymentService paymentService,
                            com.fullmall.order.app.LineRefundService lineRefunds,
                            com.fullmall.distribution.app.CommissionHoldService commissionHold,
                            com.fullmall.notification.app.NotificationService notifications,
                            com.fullmall.inventory.app.InventoryService inventoryService,
                            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.lineRefunds = lineRefunds;
        this.commissionHold = commissionHold;
        this.notifications = notifications;
        this.inventoryService = inventoryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public AfterSaleView create(UUID orderId, CreateAfterSaleRequest request) {
        Order order = orderService.getOrder(orderId);
        if (!order.contactPhone().equals(request.contactPhone().trim())) {
            throw ApiException.conflict("AFTER_SALE_PHONE_MISMATCH", "手机号与订单联系人不一致");
        }
        if (order.status() != OrderStatus.PAID
                && order.status() != OrderStatus.ALLOCATED
                && order.status() != OrderStatus.PARTIALLY_FULFILLED
                && order.status() != OrderStatus.FULFILLED) {
            throw ApiException.conflict("AFTER_SALE_ORDER_STATE", "订单当前状态不可申请售后：" + order.status());
        }
        if (repository.countActive(orderId) > 0) {
            throw ApiException.conflict("AFTER_SALE_ACTIVE_EXISTS", "该订单已有处理中的售后单");
        }
        String type = request.type().trim().toUpperCase();
        if (!"RETURN".equals(type) && !"EXCHANGE".equals(type)) {
            throw ApiException.badRequest("AFTER_SALE_TYPE_INVALID", "售后类型必须是 RETURN 或 EXCHANGE");
        }

        Set<UUID> seen = new HashSet<>();
        long refundAmount = 0;
        for (CreateAfterSaleRequest.Item item : request.items()) {
            if (!seen.add(item.orderLineId())) {
                throw ApiException.badRequest("AFTER_SALE_DUPLICATE_LINE", "同一订单行不能重复提交");
            }
            OrderLine line = order.lines().stream()
                    .filter(candidate -> candidate.id().equals(item.orderLineId()))
                    .findFirst()
                    .orElseThrow(() -> ApiException.badRequest("AFTER_SALE_LINE_NOT_FOUND",
                            "订单行不存在：" + item.orderLineId()));
            if (item.quantity() > line.quantity()) {
                throw ApiException.badRequest("AFTER_SALE_QUANTITY_EXCEEDED",
                        "商品 " + line.title() + " 最多可申请 " + line.quantity() + " 件");
            }
            if ("RETURN".equals(type)) {
                long subtotal = order.subtotal().amount();
                long gross = line.unitPrice().amount() * item.quantity();
                long net = subtotal == 0
                        ? 0
                        : (gross * order.total().amount() + subtotal / 2) / subtotal;
                refundAmount = Math.addExact(refundAmount, net);
            }
        }
        if (refundAmount > 0
                && repository.sumRefunded(orderId) + refundAmount > order.total().amount()) {
            throw ApiException.conflict("AFTER_SALE_REFUND_EXCEEDED", "退款金额超过订单可退金额");
        }

        UUID id = Ids.newId();
        String afterSaleNo = "AS-" + Ids.orderNo();
        repository.insertAfterSale(id, afterSaleNo, orderId, type, request.reason().trim(),
                request.description(), request.contactPhone().trim(), refundAmount, order.currency());
        for (CreateAfterSaleRequest.Item item : request.items()) {
            OrderLine line = order.lines().stream()
                    .filter(candidate -> candidate.id().equals(item.orderLineId()))
                    .findFirst()
                    .orElseThrow();
            repository.insertLine(id, item.orderLineId(), line.skuId(), item.quantity());
        }
        repository.insertEvent(id, "REQUESTED", request.reason().trim(), "顾客");
        // 部分退货：冻结对应比例佣金，售后关闭前不得提现（DEFER 口径）
        if (refundAmount > 0) {
            commissionHold.hold(orderId, id, refundAmount, order.total().amount());
        }
        notifyOrder(orderId, "AFTER_SALE_CREATED", "售后申请已提交",
                "售后单 " + afterSaleNo + " 已提交，我们会尽快处理。");
        return repository.find(id);
    }

    /** 分页列表（统一契约 items/total/page/size）。 */
    public java.util.Map<String, Object> list(String status, int page, int size) {
        return list(status, page, size, null, null);
    }

    public java.util.Map<String, Object> list(String status, int page, int size,
                                              String sort, String direction) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        List<AfterSaleView> items = repository.list(status, safePage, safeSize, sort, direction);
        long total = repository.countByStatus(status);
        return java.util.Map.of("items", items, "page", safePage, "size", safeSize, "total", total);
    }

    public List<AfterSaleView> list(String status) {
        return repository.list(status, 200);
    }

    public List<AfterSaleView> listByOrder(UUID orderId, String phone) {
        if (phone == null || phone.isBlank()) {
            throw ApiException.badRequest("AFTER_SALE_PHONE_REQUIRED", "查询售后需要订单联系人手机号");
        }
        Order order = orderService.getOrder(orderId);
        if (!order.contactPhone().equals(phone.trim())) {
            throw ApiException.conflict("AFTER_SALE_PHONE_MISMATCH", "手机号与订单联系人不一致");
        }
        return repository.listByOrder(orderId, phone.trim());
    }


    /** 顾客申报"未收到换货商品"：阻止自动完成并转人工。 */
    @org.springframework.transaction.annotation.Transactional
    public void reportNotReceived(java.util.UUID orderId, java.util.UUID afterSaleId, String phone) {
        var order = orderService.getOrder(orderId);
        if (phone == null || !order.contactPhone().equals(phone.trim())) {
            throw ApiException.conflict("AFTER_SALE_PHONE_MISMATCH", "手机号与订单联系人不一致");
        }
        repository.markDisputed(afterSaleId, "顾客申报未收到换货商品（" + phone.trim() + "）");
        repository.insertEvent(afterSaleId, "DISPUTED", "顾客申报未收到换货商品", "顾客");
    }

    /** 换货自动完成时解除佣金冻结（换货不属于退货，不扣佣金）。 */
    public void closeCommissionHoldForAutoComplete(java.util.UUID afterSaleId) {
        commissionHold.close(afterSaleId, false);
    }

    /**
     * 换货发货：记录换货商品的承运商与运单号，状态推进到 `EXCHANGE_SHIPPED`，并通知顾客。
     *
     * <p>前置：售后类型必须是 `EXCHANGE`，状态必须是 `RECEIVED`（已收到顾客寄回的商品）。
     * 承运商与运单号必填。
     *
     * <p>已知限制：本轮只记录换货物流并通知；**换货商品的库存出库**尚未接入履约模块，
     * 因此不会生成库存流水。接入前换货不应被视为已完成（状态停在 `EXCHANGE_SHIPPED`）。
     */
    @Transactional
    public AfterSaleView shipExchange(UUID afterSaleId, String carrierCode, String trackingNo) {
        if (carrierCode == null || carrierCode.isBlank()) {
            throw ApiException.badRequest("CARRIER_REQUIRED", "必须填写承运商");
        }
        if (trackingNo == null || trackingNo.isBlank()) {
            throw ApiException.badRequest("TRACKING_NO_REQUIRED", "必须填写运单号");
        }
        AfterSaleView afterSale = require(afterSaleId);
        if (!"EXCHANGE".equals(afterSale.type())) {
            throw ApiException.conflict("AFTER_SALE_NOT_EXCHANGE",
                    "只有换货售后可以发换货商品，当前类型=" + afterSale.type());
        }
        if (!"RECEIVED".equals(afterSale.status())) {
            throw ApiException.conflict("AFTER_SALE_NOT_RECEIVED",
                    "需先收到顾客寄回的商品才能发出换货，当前状态=" + afterSale.status());
        }
        dispatchExchangeStock(afterSale, afterSaleId);
        repository.markExchangeShipped(afterSaleId, carrierCode.trim(), trackingNo.trim(),
                exchangeAutoCompleteDays);
        repository.insertEvent(afterSaleId, "EXCHANGE_SHIPPED",
                "换货已发出，承运商 " + carrierCode.trim() + "，运单号 " + trackingNo.trim(), "system");
        notifyOrder(afterSale.orderId(), "AFTER_SALE_EXCHANGE_SHIPPED", "换货商品已发出",
                "您的换货商品已发出，承运商 " + carrierCode.trim() + "，运单号 " + trackingNo.trim() + "。");
        return require(afterSaleId);
    }

    /**
     * 售后通知：走 `notifyOrderEvent`，它会按订单归属自动分流——
     * 会员订单写 `MEMBER/<id>`，游客订单写 `CUSTOMER/<手机号>`。
     *
     * <p>此前只通知会员，游客订单的售后消息**一条都收不到**（真缺口，已修）。
     */
    private void notifyOrder(UUID orderId, String eventType, String title, String content) {
        notifications.notifyOrderEvent(orderId, eventType, title, content, List.of("IN_APP"));
    }

    @Transactional
    public AfterSaleView approve(UUID afterSaleId, ApproveAfterSaleRequest request, String operator) {
        AfterSaleView afterSale = require(afterSaleId);
        if (!"REQUESTED".equals(afterSale.status())) {
            throw ApiException.conflict("AFTER_SALE_NOT_APPROVABLE", "售后单当前状态为 " + afterSale.status());
        }
        repository.approve(afterSaleId, request.adminNote());
        repository.insertEvent(afterSaleId, "APPROVED", request.adminNote(), operator);
        notifyOrder(afterSale.orderId(), "AFTER_SALE_APPROVED", "售后已通过审核",
                "售后单 " + afterSale.afterSaleNo() + " 已通过审核，请按提示寄回商品。");
        return require(afterSaleId);
    }

    @Transactional
    public AfterSaleView reject(UUID afterSaleId, RejectAfterSaleRequest request, String operator) {
        AfterSaleView afterSale = require(afterSaleId);
        if (!"REQUESTED".equals(afterSale.status()) && !"APPROVED".equals(afterSale.status())) {
            throw ApiException.conflict("AFTER_SALE_NOT_REJECTABLE", "售后单当前状态为 " + afterSale.status());
        }
        commissionHold.close(afterSaleId, false);   // 驳回：解除冻结，不扣佣金
        repository.reject(afterSaleId, request.reason().trim());
        repository.insertEvent(afterSaleId, "REJECTED", request.reason().trim(), operator);
        return require(afterSaleId);
    }

    @Transactional
    public AfterSaleView receive(UUID afterSaleId, ReceiveAfterSaleRequest request, String operator) {
        AfterSaleView afterSale = require(afterSaleId);
        if (!"APPROVED".equals(afterSale.status())) {
            throw ApiException.conflict("AFTER_SALE_NOT_RECEIVABLE", "售后单当前状态为 " + afterSale.status());
        }
        String locationType;
        try {
            locationType = LocationType.valueOf(request.locationType().trim().toUpperCase()).name();
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("AFTER_SALE_LOCATION_INVALID", "退货入库地点类型无效");
        }
        for (var line : afterSale.lines()) {
            String batchNo = "AS-" + afterSale.afterSaleNo() + "-" + line.id().toString().substring(0, 8);
            UUID batchId = repository.insertQuarantineBatch(line.skuId(), locationType, request.locationId(),
                    batchNo, line.quantity(), afterSale.refundAmount().currency());
            repository.insertStockMove(batchId, line.skuId(), locationType, request.locationId(),
                    line.quantity(), afterSaleId);
        }
        repository.markReceived(afterSaleId);
        repository.insertEvent(afterSaleId, "RECEIVED", "退货入库，批次状态 QUARANTINE", operator);
        return require(afterSaleId);
    }

    @Transactional
    public AfterSaleView refund(UUID afterSaleId, String operator) {
        AfterSaleView afterSale = require(afterSaleId);
        if (!"RECEIVED".equals(afterSale.status())) {
            throw ApiException.conflict("AFTER_SALE_NOT_REFUNDABLE", "售后单当前状态为 " + afterSale.status());
        }
        long amount = afterSale.refundAmount().amount();
        if (amount > 0) {
            paymentService.refund(afterSale.orderId(), amount, false);
            // 落行退款记录：让"退的是哪几件商品"可追溯，并供后续按比例回退使用
            lineRefunds.recordForAfterSale(afterSaleId);
        }
        // 售后结果=退货完成/已退款 → 按冻结额回冲佣金
        commissionHold.close(afterSaleId, true);
        repository.markRefunded(afterSaleId);
        repository.insertEvent(afterSaleId, "REFUNDED",
                amount > 0 ? "原路退款 " + amount + " 分" : "换货完成，无退款", operator);
        notifyOrder(afterSale.orderId(), "AFTER_SALE_REFUNDED", "售后已退款",
                amount > 0 ? "退款 " + amount + " 分已原路退回，请留意到账。" : "换货已完成。");
        if (amount > 0) {
            Order order = orderService.getOrder(afterSale.orderId());
            long refunded = repository.sumRefunded(afterSale.orderId());
            orderService.markAfterSaleRefunded(afterSale.orderId(), refunded >= order.total().amount());
        }
        return require(afterSaleId);
    }

    /**
     * 换货出库：为售后涉及的行**再次预留**并立即发货（预留表没有 (order,line) 唯一键，支持多轮）。
     *
     * <p>只发货本次新增的预留（`shipReservations` 只处理 `CONFIRMED` 状态，历史已发货的不会重复扣减）。
     */
    private void dispatchExchangeStock(AfterSaleView afterSale, UUID afterSaleId) {
        if (afterSale.lines().isEmpty()) {
            // 没有行明细无法确定换货商品，跳过出库（历史数据/异常数据），不阻断发货
            org.slf4j.LoggerFactory.getLogger(AfterSaleService.class)
                    .warn("换货售后没有商品行，跳过库存出库 afterSaleId={}", afterSaleId);
            return;
        }
        List<UUID> lineIds = afterSale.lines().stream()
                .map(com.fullmall.aftersale.api.AfterSaleDtos.AfterSaleLineView::orderLineId)
                .toList();
        for (var line : afterSale.lines()) {
            inventoryService.reserveForOrderLine(afterSale.orderId(), line.orderLineId(),
                    line.skuId(), line.quantity(),
                    com.fullmall.inventory.domain.LocationType.WAREHOUSE,
                    warehouseIdFor(afterSale.orderId()),
                    com.fullmall.catalog.domain.ShelfLifePolicy.none());
        }
        inventoryService.confirmOrderReservations(afterSale.orderId());
        inventoryService.shipReservationsForLines(afterSale.orderId(), lineIds);
    }

    /** 取订单首个履约仓；没有仓（如门店自提）时退回门店仓。 */
    private UUID warehouseIdFor(UUID orderId) {
        var ids = jdbcTemplate.queryForList("""
                SELECT location_id FROM inventory_batch b
                 WHERE b.location_type = 'WAREHOUSE'
                 ORDER BY b.created_at LIMIT 1
                """, UUID.class);
        if (ids.isEmpty()) {
            throw ApiException.conflict("NO_WAREHOUSE_LOCATION",
                    "没有可用的仓库位置，无法为换货出库：" + orderId);
        }
        return ids.get(0);
    }

    /**
     * 完成换货：顾客确认收到换货商品后，由门店/客服标记完成。
     *
     * <p>为什么不自动完成：系统无法感知顾客何时真正收到换货（没有签收回调），
     * 自动完成会让"未收到货却已完成"的售后无法追溯。
     */
    @Transactional
    public AfterSaleView completeExchange(UUID afterSaleId, String operator) {
        if (operator == null || operator.isBlank()) {
            throw ApiException.badRequest("OPERATOR_REQUIRED", "必须记录操作人");
        }
        AfterSaleView afterSale = require(afterSaleId);
        if (!"EXCHANGE".equals(afterSale.type())) {
            throw ApiException.conflict("AFTER_SALE_NOT_EXCHANGE",
                    "只有换货售后可以标记换货完成");
        }
        if (!"EXCHANGE_SHIPPED".equals(afterSale.status())) {
            throw ApiException.conflict("AFTER_SALE_NOT_SHIPPED",
                    "换货尚未发出，不能标记完成，当前状态=" + afterSale.status());
        }
        commissionHold.close(afterSaleId, false);   // 换货完成：非退货，不扣佣金
        repository.completeExchange(afterSaleId, operator);
        repository.insertEvent(afterSaleId, "EXCHANGE_COMPLETED", "换货已完成", operator);
        notifyOrder(afterSale.orderId(), "AFTER_SALE_EXCHANGE_COMPLETED", "换货已完成",
                "您的换货已完成，感谢您的耐心等待。");
        return require(afterSaleId);
    }

    private AfterSaleView require(UUID afterSaleId) {
        AfterSaleView afterSale = repository.find(afterSaleId);
        if (afterSale == null) {
            throw ApiException.notFound("AFTER_SALE_NOT_FOUND", "售后单不存在：" + afterSaleId);
        }
        return afterSale;
    }
}