package com.fullmall.store.api;

import com.fullmall.store.app.StoreAdminService;
import com.fullmall.store.app.StoreService;
import com.fullmall.store.domain.Store;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 门店后台管理。
 *
 * <p>鉴权走 {@code StaffAuthFilter} 的路径映射（/admin/v1/stores → staff.manage），
 * 与员工管理同权限：门店绑定与门店资料本来就是同一批运营动作。
 */
@RestController
@RequestMapping("/admin/v1/stores")
public class AdminStoreController {

    private final StoreService storeService;
    private final StoreAdminService adminService;

    public AdminStoreController(StoreService storeService, StoreAdminService adminService) {
        this.storeService = storeService;
        this.adminService = adminService;
    }

    public record StoreRequest(String code, String marketCode, String name, String address,
                               BigDecimal latitude, BigDecimal longitude, Boolean pickupEnabled,
                               String phone, String openingHours) {
    }

    public record ActiveRequest(Boolean active) {
    }

    /**
     * 默认只返回营业中的门店：员工绑定门店的下拉与门店选择器不该出现已停用项。
     * 门店管理页显式传 includeInactive=true 才能看到历史门店。
     */
    @GetMapping
    public List<Store> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return includeInactive ? adminService.list(true) : storeService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Store create(@RequestBody StoreRequest request, Authentication authentication) {
        return adminService.create(toCommand(request), authentication.getName());
    }

    @PutMapping("/{id}")
    public Store update(@PathVariable UUID id, @RequestBody StoreRequest request,
                        Authentication authentication) {
        return adminService.update(id, toCommand(request), authentication.getName());
    }

    @PostMapping("/{id}/active")
    public Store setActive(@PathVariable UUID id, @RequestBody ActiveRequest request,
                           Authentication authentication) {
        if (request.active() == null) {
            // 布尔业务状态不允许"缺省即停用"：漏传字段必须失败，否则一次手抖就下线门店
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "STORE_ACTIVE_REQUIRED", "必须显式指定 active=true/false");
        }
        return adminService.setActive(id, request.active(), authentication.getName());
    }

    private StoreAdminService.Command toCommand(StoreRequest request) {
        if (request.pickupEnabled() == null) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "STORE_PICKUP_REQUIRED", "必须显式指定是否支持自提（pickupEnabled）");
        }
        return new StoreAdminService.Command(request.code(), request.marketCode(), request.name(),
                request.address(), request.latitude(), request.longitude(),
                request.pickupEnabled(), request.phone(), request.openingHours());
    }
}