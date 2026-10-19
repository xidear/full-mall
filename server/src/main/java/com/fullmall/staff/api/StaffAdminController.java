package com.fullmall.staff.api;

import com.fullmall.staff.domain.StaffUser;
import com.fullmall.staff.api.StaffPinRequest.BindStore;
import com.fullmall.staff.api.StaffPinRequest.SetPin;
import com.fullmall.staff.app.StaffAuthService;
import com.fullmall.staff.infra.StaffAuthRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/v1/staff")
public class StaffAdminController {

    private final StaffAuthRepository repository;
    private final StaffAuthService authService;

    public StaffAdminController(StaffAuthRepository repository, StaffAuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    @GetMapping
    public List<StaffUser> list() {
        return repository.findAll();
    }

    @PostMapping("/{staffId}/pin")
    public StaffUser setPin(@PathVariable java.util.UUID staffId,
                            @RequestBody SetPin request) {
        return authService.setPin(staffId, request.pin());
    }

    @PostMapping("/{staffId}/stores")
    public List<java.util.UUID> bindStore(@PathVariable java.util.UUID staffId,
                                          @RequestBody BindStore request) {
        authService.bindStore(staffId, request.storeId());
        return authService.storeIds(staffId);
    }

    @PostMapping("/{staffId}/stores/unbind")
    public List<java.util.UUID> unbindStore(@PathVariable java.util.UUID staffId,
                                            @RequestBody BindStore request) {
        authService.unbindStore(staffId, request.storeId());
        return authService.storeIds(staffId);
    }

    @GetMapping("/{staffId}/stores")
    public List<java.util.UUID> stores(@PathVariable java.util.UUID staffId) {
        return authService.storeIds(staffId);
    }
}