package com.fullmall.inventory.app;

import com.fullmall.catalog.domain.ShelfLifePolicy;
import com.fullmall.inventory.domain.AllocationPlan;
import com.fullmall.inventory.domain.FefoAllocator;
import com.fullmall.inventory.domain.InventoryBatch;
import com.fullmall.inventory.domain.LocationAvailability;
import com.fullmall.inventory.domain.LocationType;
import com.fullmall.inventory.domain.Reservation;
import com.fullmall.inventory.domain.ReservationAllocation;
import com.fullmall.inventory.domain.ReservationStatus;
import com.fullmall.inventory.domain.StockMove;
import com.fullmall.inventory.infra.InventoryRepository;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.util.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository repository;
    private final long reservationExpiryMinutes;

    public InventoryService(InventoryRepository repository,
                            @Value("${app.reservation.expiry-minutes:30}") long reservationExpiryMinutes) {
        this.repository = repository;
        this.reservationExpiryMinutes = reservationExpiryMinutes;
    }

    @Transactional
    public List<ReservationAllocation> reserveForOrderLine(UUID orderId, UUID orderLineId, UUID skuId, int quantity,
                                                           LocationType locationType, UUID locationId, ShelfLifePolicy policy) {
        ShelfLifePolicy effectivePolicy = policy == null ? ShelfLifePolicy.none() : policy;
        LocalDate minimumExpiry = LocalDate.now().plusDays(effectivePolicy.minRemainingShelfLifeDays());
        List<InventoryBatch> batches = repository.findAvailableBatches(skuId, locationType, locationId, minimumExpiry);
        AllocationPlan plan = FefoAllocator.plan(batches, quantity, minimumExpiry);
        if (!plan.isComplete()) {
            int available = quantity - plan.shortfall();
            throw ApiException.conflict("INSUFFICIENT_STOCK",
                    "Insufficient sellable stock for sku " + skuId + " at " + locationType + "/" + locationId
                            + ": requested " + quantity + ", available " + available);
        }

        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(reservationExpiryMinutes));
        for (ReservationAllocation allocation : plan.allocations()) {
            boolean reserved = repository.reserve(allocation.batchId(), allocation.quantity(), minimumExpiry);
            if (!reserved) {
                throw ApiException.conflict("STOCK_RESERVATION_RACE",
                        "Stock changed while reserving sku " + skuId + ", batch " + allocation.batchId());
            }
            repository.insertReservation(new Reservation(
                    Ids.newId(),
                    orderId,
                    orderLineId,
                    allocation.batchId(),
                    allocation.quantity(),
                    ReservationStatus.RESERVED,
                    expiresAt));
            repository.insertStockMove(new StockMove(
                    allocation.batchId(),
                    skuId,
                    locationType,
                    locationId,
                    "RESERVE",
                    allocation.quantity(),
                    "ORDER",
                    orderId));
        }
        return plan.allocations();
    }

    public List<LocationAvailability> findAvailableLocations(UUID skuId, ShelfLifePolicy policy) {
        ShelfLifePolicy effectivePolicy = policy == null ? ShelfLifePolicy.none() : policy;
        LocalDate minimumExpiry = LocalDate.now().plusDays(effectivePolicy.minRemainingShelfLifeDays());
        return repository.findAvailableLocations(skuId, minimumExpiry);
    }

    public List<InventoryBatch> findBatches(LocationType locationType, UUID locationId, UUID skuId,
                                            Integer expiringInDays) {
        return repository.findBatches(locationType, locationId, skuId, expiringInDays);
    }

    @Transactional
    public void confirmOrderReservations(UUID orderId) {
        repository.confirmOrderReservations(orderId);
    }

    @Transactional
    public void releaseOrder(UUID orderId) {
        List<Reservation> reservations = repository.findReservationsByOrder(orderId);
        for (Reservation reservation : reservations) {
            if (reservation.status() != ReservationStatus.RESERVED
                    && reservation.status() != ReservationStatus.CONFIRMED) {
                continue;
            }
            if (!repository.release(reservation.batchId(), reservation.quantity())) {
                throw new IllegalStateException("Failed to release reservation " + reservation.id()
                        + " for batch " + reservation.batchId());
            }
            repository.updateReservationStatus(reservation.id(), reservation.status(), ReservationStatus.RELEASED);
            repository.insertStockMove(new StockMove(
                    reservation.batchId(),
                    null,
                    null,
                    null,
                    "RELEASE",
                    reservation.quantity(),
                    "ORDER",
                    orderId));
        }
    }

    @Transactional
    public void shipOrder(UUID orderId) {
        shipReservations(repository.findReservationsByOrder(orderId), orderId);
    }

    @Transactional
    public void shipReservationsForLines(UUID orderId, List<UUID> orderLineIds) {
        List<Reservation> reservations = repository.findReservationsByOrderLines(orderId, orderLineIds);
        if (reservations.isEmpty()) {
            throw ApiException.conflict("NO_CONFIRMED_RESERVATIONS",
                    "No confirmed reservations for order " + orderId + " lines " + orderLineIds);
        }
        shipReservations(reservations, orderId);
    }

    private void shipReservations(List<Reservation> reservations, UUID orderId) {
        boolean shippedAny = false;
        for (Reservation reservation : reservations) {
            if (reservation.status() != ReservationStatus.CONFIRMED) {
                continue;
            }
            if (!repository.ship(reservation.batchId(), reservation.quantity())) {
                throw ApiException.conflict("STOCK_SHIP_FAILED",
                        "Failed to ship reservation " + reservation.id() + " for batch " + reservation.batchId());
            }
            repository.updateReservationStatus(reservation.id(), ReservationStatus.CONFIRMED, ReservationStatus.SHIPPED);
            repository.insertStockMove(new StockMove(
                    reservation.batchId(),
                    null,
                    null,
                    null,
                    "SHIP",
                    reservation.quantity(),
                    "ORDER",
                    orderId));
            shippedAny = true;
        }
        if (!shippedAny) {
            throw ApiException.conflict("NO_CONFIRMED_RESERVATIONS",
                    "No confirmed reservations to ship for order " + orderId);
        }
    }

    @Transactional
    public void releaseExpiredReservations() {
        List<Reservation> expired = repository.findExpiredReservations();
        for (Reservation reservation : expired) {
            if (!repository.release(reservation.batchId(), reservation.quantity())) {
                throw new IllegalStateException("Failed to release expired reservation " + reservation.id());
            }
            repository.updateReservationStatus(reservation.id(), ReservationStatus.RESERVED, ReservationStatus.RELEASED);
            repository.insertStockMove(new StockMove(
                    reservation.batchId(),
                    null,
                    null,
                    null,
                    "RELEASE_EXPIRED",
                    reservation.quantity(),
                    "RESERVATION",
                    reservation.id()));
            log.info("Released expired reservation id={} orderId={}", reservation.id(), reservation.orderId());
        }
    }
}