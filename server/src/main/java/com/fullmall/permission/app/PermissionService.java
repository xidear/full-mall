package com.fullmall.permission.app;

import com.fullmall.permission.api.PermissionDtos.PermissionMatrixView;
import com.fullmall.permission.api.PermissionDtos.RolePermissionView;
import com.fullmall.permission.api.PermissionDtos.SaveRolePermissionsRequest;
import com.fullmall.permission.infra.PermissionRepository;
import com.fullmall.platform.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PermissionService {

    private static final List<String> EDITABLE_ROLES = List.of("OPERATOR", "STORE", "CS");
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private final PermissionRepository repository;
    private final ConcurrentHashMap<String, CachedPermissions> cache = new ConcurrentHashMap<>();

    public PermissionService(PermissionRepository repository) {
        this.repository = repository;
    }

    public boolean hasPermission(String role, String permissionCode) {
        if ("ADMIN".equals(role)) {
            return true;
        }
        if (permissionCode == null) {
            return true;
        }
        return permissionsFor(role).contains(permissionCode);
    }

    public String requiredPermission(String path) {
        if (path.startsWith("/admin/v1/auth/")) {
            return null;
        }
        if (path.startsWith("/admin/v1/dashboard")) return "dashboard.view";
        if (path.startsWith("/admin/v1/products")) return "product.manage";
        if (path.startsWith("/admin/v1/orders")) return "order.manage";
        if (path.startsWith("/admin/v1/customers")) return "customer.view";
        if (path.startsWith("/admin/v1/inventory")) return "inventory.view";
        if (path.startsWith("/admin/v1/warehouse")) return "warehouse.manage";
        if (path.startsWith("/admin/v1/pos")) return "pos.manage";
        if (path.startsWith("/admin/v1/promotions")) return "promotion.manage";
        if (path.startsWith("/admin/v1/distribution")) return "promotion.manage";
        if (path.startsWith("/admin/v1/after-sales")) return "order.manage";
        if (path.startsWith("/admin/v1/suppliers")) return "supplier.manage";
        if (path.startsWith("/admin/v1/notifications")) return "notification.view";
        if (path.startsWith("/admin/v1/theme")) return "theme.manage";
        if (path.startsWith("/admin/v1/staff")) return "staff.manage";
        if (path.startsWith("/admin/v1/stores")) return "staff.manage";
        if (path.startsWith("/admin/v1/settings")) return "settings.manage";
        if (path.startsWith("/admin/v1/permissions")) return "permission.manage";
        return "permission.manage";
    }

    public PermissionMatrixView matrix() {
        List<RolePermissionView> roles = new ArrayList<>();
        for (String role : EDITABLE_ROLES) {
            roles.add(new RolePermissionView(role, repository.rolePermissions(role)));
        }
        return new PermissionMatrixView(repository.permissions(), roles);
    }

    @Transactional
    public RolePermissionView saveRole(String role, SaveRolePermissionsRequest request) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        if (!EDITABLE_ROLES.contains(normalized)) {
            throw ApiException.badRequest("PERMISSION_ROLE_NOT_EDITABLE",
                    "角色 " + role + " 不可编辑；ADMIN 拥有全部权限");
        }
        List<String> codes = new ArrayList<>(new HashSet<>(request.codes()));
        for (String code : codes) {
            if (!repository.permissionExists(code)) {
                throw ApiException.badRequest("PERMISSION_CODE_UNKNOWN", "未知权限编码：" + code);
            }
        }
        repository.replaceRolePermissions(normalized, codes);
        cache.remove(normalized);
        return new RolePermissionView(normalized, codes);
    }

    private Set<String> permissionsFor(String role) {
        CachedPermissions cached = cache.get(role);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.codes();
        }
        Set<String> codes = new HashSet<>(repository.rolePermissions(role));
        cache.put(role, new CachedPermissions(codes, Instant.now().plus(CACHE_TTL)));
        return codes;
    }

    private record CachedPermissions(Set<String> codes, Instant expiresAt) {
    }
}