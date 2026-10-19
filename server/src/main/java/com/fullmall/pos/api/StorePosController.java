package com.fullmall.pos.api;

import com.fullmall.pos.app.PosService;
import com.fullmall.staff.domain.StaffUser;
import com.fullmall.staff.web.StaffAuthFilter;
import com.fullmall.store.app.StoreAccessService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/store/v1/pos")
public class StorePosController {

    private final PosService posService;
    private final StoreAccessService storeAccessService;

    public StorePosController(PosService posService, StoreAccessService storeAccessService) {
        this.posService = posService;
        this.storeAccessService = storeAccessService;
    }

    @GetMapping("/providers")
    public List<PosProviderView> providers() {
        return posService.providers();
    }

    @GetMapping("/products")
    public List<PosProductView> products(@RequestParam UUID storeId,
                                         @RequestParam(required = false) String keyword) {
        requireStore(storeId);
        return posService.searchProducts(storeId, keyword);
    }

    @GetMapping("/members")
    public PosMemberView member(@RequestParam String query) {
        return posService.lookupMember(query);
    }

    @PostMapping("/shifts")
    public PosShiftView openShift(@Valid @RequestBody OpenShiftRequest request) {
        requireStore(request.storeId());
        return posService.openShift(request);
    }

    @GetMapping("/shifts/open")
    public ResponseEntity<PosShiftView> findOpenShift(@RequestParam UUID storeId,
                                                      @RequestParam String cashierName) {
        requireStore(storeId);
        PosShiftView shift = posService.findOpenShift(storeId, cashierName);
        return shift == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(shift);
    }

    @GetMapping("/shifts/{shiftId}")
    public PosShiftView shift(@PathVariable UUID shiftId) {
        requireShift(shiftId);
        return posService.getShift(shiftId);
    }

    @PostMapping("/shifts/{shiftId}/close")
    public PosShiftView closeShift(@PathVariable UUID shiftId,
                                   @Valid @RequestBody CloseShiftRequest request) {
        requireShift(shiftId);
        return posService.closeShift(shiftId, request);
    }

    @PostMapping("/shifts/{shiftId}/cash-movements")
    public PosShiftView cashMovement(@PathVariable UUID shiftId,
                                     @Valid @RequestBody CashMovementRequest request) {
        requireShift(shiftId);
        return posService.cashMovement(shiftId, request);
    }

    @GetMapping("/shifts/{shiftId}/sales")
    public List<PosSaleView> shiftSales(@PathVariable UUID shiftId) {
        requireShift(shiftId);
        return posService.findShiftSales(shiftId);
    }

    @PostMapping("/sales")
    public PosSaleView createSale(@Valid @RequestBody CreatePosSaleRequest request,
                                  @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        requireStore(request.storeId());
        return posService.createSale(request, idempotencyKey);
    }

    @GetMapping("/sales")
    public List<PosSaleView> sales(@RequestParam UUID storeId,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        requireStore(storeId);
        return posService.findStoreSales(storeId, date);
    }

    @GetMapping("/sales/{saleId}")
    public PosSaleView sale(@PathVariable UUID saleId) {
        requireSale(saleId);
        return posService.getSale(saleId);
    }

    @PostMapping("/sales/{saleId}/member")
    public PosSaleView attachMember(@PathVariable UUID saleId,
                                    @Valid @RequestBody AttachMemberRequest request) {
        requireSale(saleId);
        return posService.attachMember(saleId, request);
    }

    @PostMapping("/sales/{saleId}/coupon")
    public PosSaleView applyCoupon(@PathVariable UUID saleId,
                                   @Valid @RequestBody ApplyCouponRequest request) {
        requireSale(saleId);
        return posService.applyCoupon(saleId, request);
    }

    @PostMapping("/sales/{saleId}/points")
    public PosSaleView redeemPoints(@PathVariable UUID saleId,
                                    @Valid @RequestBody RedeemPointsRequest request) {
        requireSale(saleId);
        return posService.redeemPoints(saleId, request);
    }

    @PostMapping("/sales/{saleId}/tenders")
    public PosSaleView addTender(@PathVariable UUID saleId,
                                 @Valid @RequestBody AddTenderRequest request) {
        requireSale(saleId);
        return posService.addTender(saleId, request);
    }

    @PostMapping("/sales/{saleId}/complete")
    public PosSaleView completeSale(@PathVariable UUID saleId) {
        requireSale(saleId);
        return posService.completeSale(saleId);
    }

    @PostMapping("/sales/{saleId}/void")
    public PosSaleView voidSale(@PathVariable UUID saleId) {
        requireSale(saleId);
        return posService.voidSale(saleId);
    }

    @PostMapping("/sales/{saleId}/refund")
    public PosSaleView refundSale(@PathVariable UUID saleId,
                                  @Valid @RequestBody RefundPosSaleRequest request) {
        requireSale(saleId);
        return posService.refundSale(saleId, request);
    }

    @PostMapping("/sales/{saleId}/refund-lines")
    public PosSaleView refundLines(@PathVariable UUID saleId,
                                   @Valid @RequestBody RefundPosLinesRequest request) {
        requireSale(saleId);
        return posService.refundLines(saleId, request);
    }

    private void requireSale(UUID saleId) {
        PosSaleView view = posService.getSale(saleId);
        requireStore(view.sale().storeId());
    }

    private void requireShift(UUID shiftId) {
        PosShiftView view = posService.getShift(shiftId);
        requireStore(view.shift().storeId());
    }

    private void requireStore(UUID storeId) {
        storeAccessService.requireStore(currentStaff(), storeId);
    }

    private StaffUser currentStaff() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object user = attributes.getAttribute(StaffAuthFilter.STAFF_USER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return user instanceof StaffUser staffUser ? staffUser : null;
    }
}