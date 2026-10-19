package com.fullmall.permission.api;

import com.fullmall.permission.api.PermissionDtos.PermissionMatrixView;
import com.fullmall.permission.api.PermissionDtos.RolePermissionView;
import com.fullmall.permission.api.PermissionDtos.SaveRolePermissionsRequest;
import com.fullmall.permission.app.PermissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public PermissionMatrixView matrix() {
        return permissionService.matrix();
    }

    @PutMapping("/roles/{role}")
    public RolePermissionView save(@PathVariable String role,
                                   @Valid @RequestBody SaveRolePermissionsRequest request) {
        return permissionService.saveRole(role, request);
    }
}
