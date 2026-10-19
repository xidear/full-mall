package com.fullmall.permission.api;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class PermissionDtos {

    private PermissionDtos() {
    }

    public record PermissionView(String code, String label, String group, int sortOrder) {
    }

    public record RolePermissionView(String role, List<String> permissions) {
    }

    public record PermissionMatrixView(List<PermissionView> permissions, List<RolePermissionView> roles) {
    }

    public record SaveRolePermissionsRequest(@NotNull List<String> codes) {
    }
}
