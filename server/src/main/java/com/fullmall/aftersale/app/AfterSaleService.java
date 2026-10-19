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

    public AfterSaleService(AfterSaleRepository repository, OrderService orderService,
                            PaymentService paymentService) {
        this.repository = repository;
        this.orderService = orderService;
        this.paymentService = paymentService;
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
        return repository.find(id);
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

    @Transactional
    public AfterSaleView approve(UUID afterSaleId, ApproveAfterSaleRequest request, String operator) {
        AfterSaleView afterSale = require(afterSaleId);
        if (!"REQUESTED".equals(afterSale.status())) {
            throw ApiException.conflict("AFTER_SALE_NOT_APPROVABLE", "售后单当前状态为 " + afterSale.status());
        }
        repository.approve(afterSaleId, request.adminNote());
        repository.insertEvent(afterSaleId, "APPROVED", request.adminNote(), operator);
        return require(afterSaleId);
    }

    @Transactional
    public AfterSaleView reject(UUID afterSaleId, RejectAfterSaleRequest request, String operator) {
        AfterSaleView afterSale = require(afterSaleId);
        if (!"REQUESTED".equals(afterSale.status()) && !"APPROVED".equals(afterSale.status())) {
            throw ApiException.conflict("AFTER_SALE_NOT_REJECTABLE", "售后单当前状态为 " + afterSale.status());
        }
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
            paymentService.refund(afterSale.orderId(), amount);
        }
        repository.markRefunded(afterSaleId);
        repository.insertEvent(afterSaleId, "REFUNDED",
                amount > 0 ? "原路退款 " + amount + " 分" : "换货完成，无退款", operator);
        if (amount > 0) {
            Order order = orderService.getOrder(afterSale.orderId());
            long refunded = repository.sumRefunded(afterSale.orderId());
            orderService.markAfterSaleRefunded(afterSale.orderId(), refunded >= order.total().amount());
        }
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