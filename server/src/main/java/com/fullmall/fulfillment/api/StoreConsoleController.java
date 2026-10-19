package com.fullmall.fulfillment.api;

import com.fullmall.fulfillment.app.FulfillmentService;
import com.fullmall.fulfillment.domain.PickTask;
import com.fullmall.fulfillment.domain.Pickup;
import com.fullmall.fulfillment.dto.PickupDetail;
import com.fullmall.staff.domain.StaffUser;
import com.fullmall.staff.web.StaffAuthFilter;
import com.fullmall.store.app.StoreAccessService;
import com.fullmall.store.domain.Store;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/store/v1")
public class StoreConsoleController {

    private final FulfillmentService fulfillmentService;
    private final StoreAccessService storeAccessService;

    public StoreConsoleController(FulfillmentService fulfillmentService,
                                  StoreAccessService storeAccessService) {
        this.fulfillmentService = fulfillmentService;
        this.storeAccessService = storeAccessService;
    }

    @GetMapping("/my-stores")
    public List<Store> myStores() {
        return storeAccessService.accessibleStores(currentStaff());
    }

    @GetMapping("/pick-tasks")
    public List<PickTask> pickTasks(@RequestParam UUID storeId,
                                    @RequestParam(required = false) String status) {
        storeAccessService.requireStore(currentStaff(), storeId);
        return fulfillmentService.findPickTasks(storeId, status);
    }

    @PostMapping("/pick-tasks/{taskId}/confirm")
    public PickTask confirmPick(@PathVariable UUID taskId,
                                @Valid @RequestBody ConfirmPickRequest request) {
        storeAccessService.requireStore(currentStaff(), fulfillmentService.getPickTask(taskId).storeId());
        return fulfillmentService.confirmPick(taskId, request.batchId(), request.quantity(), request.scannedCode());
    }

    @PostMapping("/pickups/{pickupCode}/verify")
    public Pickup verifyPickup(@PathVariable String pickupCode,
                               @Valid @RequestBody VerifyPickupRequest request) {
        return fulfillmentService.verifyPickup(pickupCode, request.verificationType(), request.verificationValue());
    }

    @GetMapping("/pickups/{pickupCode}")
    public PickupDetail pickup(@PathVariable String pickupCode) {
        storeAccessService.requireStore(currentStaff(), fulfillmentService.findPickup(pickupCode).storeId());
        return fulfillmentService.findPickupDetail(pickupCode);
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