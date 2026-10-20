package com.fullmall.fulfillment.app;

import com.fullmall.fulfillment.dto.PickupDetail;
import com.fullmall.fulfillment.domain.FulfillmentCompletedEvent;
import com.fullmall.fulfillment.domain.FulfillmentLine;
import com.fullmall.fulfillment.domain.FulfillmentOrder;
import com.fullmall.fulfillment.domain.FulfillmentStatus;
import com.fullmall.fulfillment.domain.PickTask;
import com.fullmall.fulfillment.domain.PickTaskLine;
import com.fullmall.fulfillment.domain.Pickup;
import com.fullmall.fulfillment.domain.PickupStatus;
import com.fullmall.fulfillment.domain.StoreTaskChangedEvent;
import com.fullmall.fulfillment.infra.FulfillmentRepository;
import com.fullmall.inventory.app.InventoryService;
import com.fullmall.inventory.domain.ReservationAllocation;
import com.fullmall.notification.app.NotificationService;
import com.fullmall.platform.domain.FulfillmentType;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.util.Ids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FulfillmentService {

    private final FulfillmentRepository repository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final long pickupExpiryHours;

    public FulfillmentService(FulfillmentRepository repository,
                              InventoryService inventoryService,
                              NotificationService notificationService,
                              ApplicationEventPublisher eventPublisher,
                              @Value("${app.pickup.expiry-hours:48}") long pickupExpiryHours) {
        this.repository = repository;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.pickupExpiryHours = pickupExpiryHours;
    }

    @Transactional
    public FulfillmentOrder createFulfillment(UUID orderId, FulfillmentType type, UUID sourceId,
                                              List<LineAllocation> lineAllocations, String contactPhone) {
        UUID fulfillmentOrderId = Ids.newId();
        String pickupCode = type == FulfillmentType.STORE_PICKUP ? Ids.pickupCode() : null;
        Instant pickupExpiresAt = type == FulfillmentType.STORE_PICKUP
                ? Instant.now().plus(Duration.ofHours(pickupExpiryHours))
                : null;

        List<FulfillmentLine> lines = new ArrayList<>();
        for (LineAllocation allocation : lineAllocations) {
            lines.add(new FulfillmentLine(
                    Ids.newId(),
                    fulfillmentOrderId,
                    allocation.orderLineId(),
                    allocation.skuId(),
                    allocation.quantity()));
        }

        FulfillmentOrder fulfillment = new FulfillmentOrder(
                fulfillmentOrderId,
                orderId,
                type,
                FulfillmentStatus.ALLOCATED,
                sourceId,
                null,
                null,
                pickupCode,
                pickupExpiresAt,
                lines);
        repository.insertFulfillmentOrder(fulfillment);
        for (FulfillmentLine line : lines) {
            repository.insertFulfillmentLine(line);
        }

        UUID taskId = Ids.newId();
        UUID storeId = switch (type) {
            case STORE_PICKUP, STORE_SHIP -> sourceId;
            case WAREHOUSE_SHIP, SUPPLIER_DROPSHIP -> null;
        };
        repository.insertPickTask(new PickTask(taskId, fulfillmentOrderId, storeId, "PENDING", List.of()));
        if (storeId != null) {
            eventPublisher.publishEvent(new StoreTaskChangedEvent(storeId, taskId, "CREATED"));
        }
        // 同一 SKU + 同一批次的多条分配（例如付费行与买赠行）必须合并为一条拣货行，
        // 否则扫码按批次确认时无法区分，拣货任务永远无法完成。
        Map<String, MergedPickLine> merged = new LinkedHashMap<>();
        for (LineAllocation allocation : lineAllocations) {
            for (ReservationAllocation reservation : allocation.allocations()) {
                String key = allocation.skuId() + "|" + reservation.batchId();
                merged.merge(key,
                        new MergedPickLine(allocation.skuId(), reservation.batchId(),
                                reservation.expiryDate(), reservation.quantity()),
                        MergedPickLine::plus);
            }
        }
        for (MergedPickLine line : merged.values()) {
            repository.insertPickTaskLine(new PickTaskLine(
                    Ids.newId(),
                    taskId,
                    line.skuId(),
                    null,
                    line.quantity(),
                    line.batchId(),
                    null,
                    line.expiryDate(),
                    0));
        }

        if (type == FulfillmentType.STORE_PICKUP) {
            repository.insertPickup(new Pickup(
                    Ids.newId(),
                    fulfillmentOrderId,
                    sourceId,
                    pickupCode,
                    PickupStatus.PENDING,
                    pickupExpiresAt,
                    null,
                    contactPhone,
                    1,
                    0));
        }

        return repository.findById(fulfillmentOrderId);
    }

    @Transactional
    public FulfillmentOrder acceptSupplierOrder(UUID fulfillmentOrderId, UUID supplierId, String note) {
        FulfillmentOrder fulfillment = requireSupplierFulfillment(fulfillmentOrderId, supplierId);
        if (fulfillment.status() != FulfillmentStatus.ALLOCATED) {
            throw ApiException.conflict("SUPPLIER_ORDER_NOT_ACCEPTABLE",
                    "供应商订单当前状态为 " + fulfillment.status() + "，不能接单");
        }
        repository.updateSupplierAcceptance(fulfillmentOrderId, note, Instant.now());
        repository.updateStatus(fulfillmentOrderId, FulfillmentStatus.ACCEPTED);
        notificationService.notifyOrderEvent(fulfillment.orderId(), "SUPPLIER_ACCEPTED", "供应商已接单",
                "您的订单已由供应商接单，将尽快发货。", List.of("IN_APP"));
        return requireFulfillment(fulfillmentOrderId);
    }

    @Transactional
    public FulfillmentOrder shipSupplierOrder(UUID fulfillmentOrderId, UUID supplierId,
                                              String carrierCode, String trackingNo) {
        FulfillmentOrder fulfillment = requireSupplierFulfillment(fulfillmentOrderId, supplierId);
        if (fulfillment.status() != FulfillmentStatus.ACCEPTED) {
            throw ApiException.conflict("SUPPLIER_ORDER_NOT_SHIPPABLE",
                    "供应商订单当前状态为 " + fulfillment.status() + "，需先接单再发货");
        }
        repository.updateShipment(fulfillmentOrderId, carrierCode, trackingNo);
        repository.updateStatus(fulfillmentOrderId, FulfillmentStatus.SHIPPED);
        inventoryService.shipReservationsForLines(fulfillment.orderId(), orderLineIds(fulfillment));
        notificationService.notifyOrderEvent(fulfillment.orderId(), "ORDER_SHIPPED", "供应商已发货",
                "您的订单已由供应商发货，运单号：" + trackingNo, List.of("IN_APP", "SMS"));
        eventPublisher.publishEvent(new FulfillmentCompletedEvent(fulfillment.orderId()));
        return requireFulfillment(fulfillmentOrderId);
    }

    @Transactional
    public FulfillmentOrder rejectSupplierOrder(UUID fulfillmentOrderId, UUID supplierId, String reason) {
        FulfillmentOrder fulfillment = requireSupplierFulfillment(fulfillmentOrderId, supplierId);
        if (fulfillment.status() != FulfillmentStatus.ALLOCATED
                && fulfillment.status() != FulfillmentStatus.ACCEPTED) {
            throw ApiException.conflict("SUPPLIER_ORDER_NOT_REJECTABLE",
                    "供应商订单当前状态为 " + fulfillment.status() + "，不能拒单");
        }
        repository.updateSupplierNote(fulfillmentOrderId, reason);
        repository.updateStatus(fulfillmentOrderId, FulfillmentStatus.CANCELLED);
        inventoryService.releaseOrderLines(fulfillment.orderId(), orderLineIds(fulfillment));
        notificationService.notifyOrderEvent(fulfillment.orderId(), "SUPPLIER_REJECTED", "供应商无法履约",
                "供应商因以下原因无法履约：" + reason + "，客服将尽快处理。", List.of("IN_APP"));
        return requireFulfillment(fulfillmentOrderId);
    }

    private FulfillmentOrder requireSupplierFulfillment(UUID fulfillmentOrderId, UUID supplierId) {
        FulfillmentOrder fulfillment = requireFulfillment(fulfillmentOrderId);
        if (fulfillment.type() != FulfillmentType.SUPPLIER_DROPSHIP
                || fulfillment.sourceId() == null
                || !fulfillment.sourceId().equals(supplierId)) {
            throw ApiException.notFound("SUPPLIER_ORDER_NOT_FOUND",
                    "供应商订单不存在：" + fulfillmentOrderId);
        }
        return fulfillment;
    }


    /**
     * 仓库/门店发货：记录承运商与运单号，并把状态推进到 `SHIPPED`。
     *
     * <p>校验：状态必须是 `ACCEPTED` / `PICKING` / `PACKED`；`carrierCode` 与 `trackingNo` **必填**
     * （没有运单号的"已发货"对顾客没有意义，也无法查件）。
     *
     * <p>发货后给订单写通知（站内 + 短信），内容带运单号。
     */
    /**
     * 接受自营履约单：ALLOCATED → ACCEPTED。
     *
     * <p>为什么必须有这一步：`shipFulfillment` 只接受 ACCEPTED/PICKING/PACKED，
     * 而自营订单初始是 ALLOCATED——缺这一步会导致**自营订单永远无法发货**。
     */
    @Transactional
    public FulfillmentOrder acceptFulfillment(UUID fulfillmentOrderId) {
        FulfillmentOrder fulfillment = requireFulfillment(fulfillmentOrderId);
        if (fulfillment.status() != FulfillmentStatus.ALLOCATED) {
            throw ApiException.conflict("FULFILLMENT_NOT_ACCEPTABLE",
                    "履约单当前状态为 " + fulfillment.status() + "，不能接受");
        }
        repository.updateStatus(fulfillmentOrderId, FulfillmentStatus.ACCEPTED);
        return requireFulfillment(fulfillmentOrderId);
    }

    /** 打包：ACCEPTED/PICKING → PACKED（发货前最后一步）。 */
    @Transactional
    public FulfillmentOrder packFulfillment(UUID fulfillmentOrderId) {
        FulfillmentOrder fulfillment = requireFulfillment(fulfillmentOrderId);
        if (fulfillment.status() != FulfillmentStatus.ACCEPTED
                && fulfillment.status() != FulfillmentStatus.PICKING) {
            throw ApiException.conflict("FULFILLMENT_NOT_PACKABLE",
                    "履约单当前状态为 " + fulfillment.status() + "，不能打包");
        }
        repository.updateStatus(fulfillmentOrderId, FulfillmentStatus.PACKED);
        return requireFulfillment(fulfillmentOrderId);
    }

    /**
     * 仓库/门店发货：记录承运商与运单号，并把状态推进到 `SHIPPED`。
     *
     * <p>校验：状态必须是 `ACCEPTED` / `PICKING` / `PACKED`；`carrierCode` 与 `trackingNo` **必填**。
     */
    @Transactional
    public FulfillmentOrder shipFulfillment(UUID fulfillmentOrderId, String carrierCode, String trackingNo) {
        if (carrierCode == null || carrierCode.isBlank()) {
            throw ApiException.badRequest("CARRIER_REQUIRED", "必须填写承运商");
        }
        if (trackingNo == null || trackingNo.isBlank()) {
            throw ApiException.badRequest("TRACKING_NO_REQUIRED", "必须填写运单号");
        }
        FulfillmentOrder fulfillment = repository.findById(fulfillmentOrderId);
        if (fulfillment.status() != FulfillmentStatus.ACCEPTED
                && fulfillment.status() != FulfillmentStatus.PICKING
                && fulfillment.status() != FulfillmentStatus.PACKED) {
            throw ApiException.conflict("FULFILLMENT_NOT_SHIPPABLE",
                    "履约单当前状态为 " + fulfillment.status() + "，不能发货");
        }
        repository.updateShipment(fulfillmentOrderId, carrierCode.trim(), trackingNo.trim());
        repository.updateStatus(fulfillmentOrderId, FulfillmentStatus.SHIPPED);
        notificationService.notifyOrderEvent(fulfillment.orderId(), "ORDER_SHIPPED", "订单已发货",
                "您的订单已发出，承运商 " + carrierCode.trim() + "，运单号 " + trackingNo.trim() + "。",
                List.of("IN_APP", "SMS"));
        return repository.findById(fulfillmentOrderId);
    }

    public List<FulfillmentOrder> findFulfillments(UUID orderId) {
        return repository.findByOrderId(orderId);
    }

    public List<PickTask> findPickTasks(UUID storeId, String status) {
        return repository.findPickTasksByStore(storeId, status);
    }

    @Transactional
    public PickTask confirmPick(UUID taskId, UUID batchId, int quantity, String scannedCode) {
        PickTask task = repository.findPickTask(taskId);
        if (task == null) {
            throw ApiException.notFound("PICK_TASK_NOT_FOUND", "Pick task not found: " + taskId);
        }
        if ("CANCELLED".equals(task.status())) {
            throw ApiException.conflict("PICK_TASK_CANCELLED", "Pick task is cancelled: " + taskId);
        }
        if ("PICKED".equals(task.status())) {
            return task;
        }

        PickTaskLine line = task.lines().stream()
                .filter(candidate -> candidate.suggestedBatchId() != null)
                .filter(candidate -> candidate.suggestedBatchId().equals(batchId))
                .findFirst()
                .orElseThrow(() -> ApiException.conflict("BATCH_NOT_IN_PICK_TASK",
                        "Batch " + batchId + " is not part of pick task " + taskId));

        if (line.pickedQuantity() > 0) {
            throw ApiException.conflict("PICK_LINE_ALREADY_CONFIRMED",
                    "Pick line already confirmed: " + line.id());
        }
        if (line.quantity() != quantity) {
            throw ApiException.conflict("PICK_QUANTITY_MISMATCH",
                    "Expected " + line.quantity() + " but received " + quantity);
        }
        if (scannedCode != null && !scannedCode.isBlank() && !scannedCode.equals(batchId.toString())) {
            throw ApiException.conflict("SCANNED_BATCH_MISMATCH",
                    "Scanned code does not match suggested batch " + batchId);
        }
        if (!repository.markPickLinePicked(line.id(), quantity)) {
            throw ApiException.conflict("PICK_LINE_ALREADY_CONFIRMED",
                    "Pick line already confirmed: " + line.id());
        }

        if (repository.countUnpicked(taskId) == 0) {
            repository.updatePickTaskStatus(taskId, "PICKED");
            if (task.storeId() != null) {
                eventPublisher.publishEvent(new StoreTaskChangedEvent(task.storeId(), taskId, "UPDATED"));
            }
            FulfillmentOrder fulfillment = requireFulfillment(task.fulfillmentOrderId());
            if (fulfillment.type() == FulfillmentType.STORE_PICKUP) {
                repository.updateStatus(fulfillment.id(), FulfillmentStatus.READY_FOR_PICKUP);
                repository.updatePickupStatusByFulfillmentId(fulfillment.id(), PickupStatus.READY);
                // 消息里必须带自提码：顾客收到短信却没有码，等于没通知
                String pickupCodeForNotice = fulfillment.pickupCode() == null ? "" : fulfillment.pickupCode();
                notificationService.notifyOrderEvent(fulfillment.orderId(), "PICKUP_READY", "备货完成，请到店自提",
                        "您的订单已备货完成，请携带提货码 " + pickupCodeForNotice + " 到门店自提。",
                        List.of("IN_APP", "SMS", "WECHAT"));
            } else {
                repository.updateShipment(fulfillment.id(), "MOCK", "MOCK" + System.currentTimeMillis());
                repository.updateStatus(fulfillment.id(), FulfillmentStatus.SHIPPED);
                inventoryService.shipReservationsForLines(fulfillment.orderId(), orderLineIds(fulfillment));
                notificationService.notifyOrderEvent(fulfillment.orderId(), "ORDER_SHIPPED", "订单已发货",
                        "您的订单已发货，请留意物流信息。", List.of("IN_APP", "SMS"));
                eventPublisher.publishEvent(new FulfillmentCompletedEvent(fulfillment.orderId()));
            }
        }
        return repository.findPickTask(taskId);
    }

    public PickTask getPickTask(UUID taskId) {
        PickTask task = repository.findPickTask(taskId);
        if (task == null) {
            throw ApiException.notFound("PICK_TASK_NOT_FOUND", "Pick task not found: " + taskId);
        }
        return task;
    }

    @Transactional
    public Pickup verifyPickup(String pickupCode, String verificationType, String verificationValue) {
        Pickup pickup = repository.findPickupByCode(pickupCode);
        if (pickup == null) {
            throw ApiException.notFound("PICKUP_NOT_FOUND", "Pickup not found: " + pickupCode);
        }
        if (pickup.status() == PickupStatus.COMPLETED) {
            return pickup;
        }
        if (pickup.status() != PickupStatus.READY && pickup.status() != PickupStatus.PARTIALLY_USED) {
            throw ApiException.conflict("PICKUP_NOT_READY", "Pickup is not ready: " + pickup.status());
        }
        if (verificationValue == null || verificationValue.isBlank()) {
            throw ApiException.badRequest("VERIFICATION_REQUIRED", "Verification value is required");
        }
        switch (verificationType) {
            case "PICKUP_CODE" -> {
                if (!pickupCode.equals(verificationValue)) {
                    throw ApiException.conflict("PICKUP_CODE_MISMATCH", "Pickup code does not match");
                }
            }
            case "PHONE" -> {
                if (!verificationValue.equals(pickup.contactPhone())) {
                    throw ApiException.conflict("PICKUP_PHONE_MISMATCH", "Phone does not match order contact");
                }
            }
            case "ID_CARD" -> throw ApiException.badRequest("ID_CARD_VERIFICATION_NOT_CONFIGURED",
                    "ID card verification is not configured in this build");
            default -> throw ApiException.badRequest("UNSUPPORTED_VERIFICATION", "Unsupported verification type");
        }

        String newStatus = repository.incrementPickupUsage(pickup.id());
        if (newStatus == null) {
            throw ApiException.conflict("PICKUP_ALREADY_COMPLETED", "Pickup has no remaining uses");
        }
        Pickup updated = repository.findPickupByCode(pickupCode);
        if (updated.status() == PickupStatus.COMPLETED) {
            FulfillmentOrder fulfillment = requireFulfillment(pickup.fulfillmentOrderId());
            repository.updateStatus(fulfillment.id(), FulfillmentStatus.COMPLETED);
            inventoryService.shipReservationsForLines(fulfillment.orderId(), orderLineIds(fulfillment));
            notificationService.notifyOrderEvent(fulfillment.orderId(), "ORDER_COMPLETED", "订单已完成",
                    "您的订单已完成，感谢您的购买。", List.of("IN_APP", "SMS"));
            eventPublisher.publishEvent(new FulfillmentCompletedEvent(fulfillment.orderId()));
        }
        return updated;
    }

    public PickupDetail findPickupDetail(String pickupCode) {
        PickupDetail detail = repository.findPickupDetail(pickupCode);
        if (detail == null) {
            throw ApiException.notFound("PICKUP_NOT_FOUND", "Pickup not found: " + pickupCode);
        }
        return detail;
    }

    public Pickup findPickup(String pickupCode) {
        Pickup pickup = repository.findPickupByCode(pickupCode);
        if (pickup == null) {
            throw ApiException.notFound("PICKUP_NOT_FOUND", "Pickup not found: " + pickupCode);
        }
        return pickup;
    }

    private FulfillmentOrder requireFulfillment(UUID fulfillmentOrderId) {
        FulfillmentOrder fulfillment = repository.findById(fulfillmentOrderId);
        if (fulfillment == null) {
            throw ApiException.notFound("FULFILLMENT_NOT_FOUND", "Fulfillment not found: " + fulfillmentOrderId);
        }
        return fulfillment;
    }

    private List<UUID> orderLineIds(FulfillmentOrder fulfillment) {
        return fulfillment.lines().stream().map(FulfillmentLine::orderLineId).toList();
    }

    private record MergedPickLine(java.util.UUID skuId, java.util.UUID batchId,
                                  java.time.LocalDate expiryDate, int quantity) {

        private MergedPickLine plus(MergedPickLine other) {
            return new MergedPickLine(skuId, batchId, expiryDate, quantity + other.quantity);
        }
    }
}
