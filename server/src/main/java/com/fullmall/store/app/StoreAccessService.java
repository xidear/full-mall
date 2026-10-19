package com.fullmall.store.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.staff.app.StaffAuthService;
import com.fullmall.staff.domain.StaffUser;
import com.fullmall.store.domain.Store;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class StoreAccessService {

    private static final Set<String> ALL_STORE_ROLES = Set.of("ADMIN", "OPERATOR");

    private final StaffAuthService staffAuthService;
    private final StoreService storeService;
    private final boolean storeAuthRequired;

    public StoreAccessService(StaffAuthService staffAuthService, StoreService storeService,
                              @Value("${app.store.auth-required:false}") boolean storeAuthRequired) {
        this.staffAuthService = staffAuthService;
        this.storeService = storeService;
        this.storeAuthRequired = storeAuthRequired;
    }

    public void requireStore(StaffUser user, UUID storeId) {
        if (storeId == null) {
            throw ApiException.badRequest("STORE_REQUIRED", "必须指定门店");
        }
        if (user == null) {
            if (storeAuthRequired) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录门店工作台");
            }
            return;
        }
        if (ALL_STORE_ROLES.contains(user.role())) {
            return;
        }
        if (!"STORE".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "当前账号没有门店工作台权限");
        }
        if (!staffAuthService.storeIds(user.id()).contains(storeId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "STORE_SCOPE_DENIED",
                    "当前账号未绑定该门店，不能访问门店 " + storeId + " 的数据");
        }
    }

    public List<Store> accessibleStores(StaffUser user) {
        if (user == null) {
            if (storeAuthRequired) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录门店工作台");
            }
            return storeService.listAll();
        }
        if (ALL_STORE_ROLES.contains(user.role())) {
            return storeService.listAll();
        }
        if (!"STORE".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "当前账号没有门店工作台权限");
        }
        List<UUID> storeIds = staffAuthService.storeIds(user.id());
        List<Store> stores = storeService.listAll().stream()
                .filter(store -> storeIds.contains(store.id()))
                .toList();
        if (stores.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "STORE_SCOPE_EMPTY",
                    "当前门店账号尚未绑定任何门店，请联系运营在员工管理中绑定");
        }
        return stores;
    }
}
