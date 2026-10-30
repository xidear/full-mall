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

    /**
     * 角色候选（含中文标签与是否可编辑），来自 role 表（ADR-0015）。
     * 前端页签据此渲染，不再写死 ADMIN/OPERATOR/STORE/CS。
     */
    @GetMapping("/roles")
    public java.util.List<java.util.Map<String, Object>> roles() {
        return permissionService.roles();
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/roles/{role}")
    public org.springframework.http.ResponseEntity<Void> deleteRole(@PathVariable String role) {
        permissionService.deleteRole(role);
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PutMapping("/roles/{role}")
    public RolePermissionView save(@PathVariable String role,
                                   @Valid @RequestBody SaveRolePermissionsRequest request) {
        return permissionService.saveRole(role, request);
    }
}
