package com.fullmall.fulfillment.api;

import com.fullmall.fulfillment.app.FulfillmentService;
import com.fullmall.fulfillment.domain.PickTask;
import com.fullmall.fulfillment.domain.Pickup;
import com.fullmall.fulfillment.dto.PickupDetail;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/store/v1")
public class StoreConsoleController {

    private final FulfillmentService fulfillmentService;

    public StoreConsoleController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping("/pick-tasks")
    public List<PickTask> pickTasks(@RequestParam UUID storeId,
                                    @RequestParam(required = false) String status) {
        return fulfillmentService.findPickTasks(storeId, status);
    }

    @PostMapping("/pick-tasks/{taskId}/confirm")
    public PickTask confirmPick(@PathVariable UUID taskId,
                                @Valid @RequestBody ConfirmPickRequest request) {
        return fulfillmentService.confirmPick(taskId, request.batchId(), request.quantity(), request.scannedCode());
    }

    @PostMapping("/pickups/{pickupCode}/verify")
    public Pickup verifyPickup(@PathVariable String pickupCode,
                               @Valid @RequestBody VerifyPickupRequest request) {
        return fulfillmentService.verifyPickup(pickupCode, request.verificationType(), request.verificationValue());
    }

    @GetMapping("/pickups/{pickupCode}")
    public PickupDetail pickup(@PathVariable String pickupCode) {
        return fulfillmentService.findPickupDetail(pickupCode);
    }
}