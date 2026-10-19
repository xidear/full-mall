package com.fullmall.supplier.api;

import com.fullmall.supplier.app.SupplierAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/suppliers")
public class AdminSupplierController {

    private final SupplierAdminService adminService;

    public AdminSupplierController(SupplierAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<SupplierCredentialView> suppliers() {
        return adminService.listSuppliers();
    }

    @PostMapping("/{supplierId}/credentials")
    public SupplierCredentialCreated rotateCredential(@PathVariable UUID supplierId) {
        return adminService.rotateCredential(supplierId);
    }
}
