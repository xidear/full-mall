package com.fullmall.staff.api;

import com.fullmall.staff.api.StaffPinRequest.BindStore;
import com.fullmall.staff.api.StaffPinRequest.SetPin;
import com.fullmall.staff.app.StaffAdminService;
import com.fullmall.staff.app.StaffAuthService;
import com.fullmall.staff.domain.StaffUser;
import com.fullmall.staff.infra.StaffAuthRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 员工管理。
 *
 * <p>写操作统一 {@code @PreAuthorize("hasRole('ADMIN')")}：员工 CRUD 是提权动作，
 * 不能只靠路径映射里那把 staff.manage（那只是"能进这一页"）。
 * 密码长度等业务规则在 {@link StaffAdminService} 里用 ValidationRules 常量判定，
 * 这里不再写一遍 @Size(min=8)，避免同一规则两处维护。
 */
@RestController
@RequestMapping("/admin/v1/staff")
public class StaffAdminController {

    private final StaffAuthRepository repository;
    private final StaffAuthService authService;
    private final StaffAdminService adminService;

    public StaffAdminController(StaffAuthRepository repository, StaffAuthService authService,
                                StaffAdminService adminService) {
        this.repository = repository;
        this.authService = authService;
        this.adminService = adminService;
    }

    public record CreateStaff(@NotBlank @Size(max = 64) String username,
                              @NotBlank String password,
                              @Size(max = 64) String displayName,
                              @NotBlank String role) {
    }

    public record UpdateStaff(@Size(max = 64) String displayName, @NotBlank String role) {
    }

    public record ChangeStatus(boolean active) {
    }

    public record ResetPassword(@NotBlank String password) {
    }

    /** 可分配的后台角色（唯一来源 StaffRoles）：前端下拉候选，不在页面写死。 */
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public List<String> roles() {
        return com.fullmall.staff.domain.StaffRoles.BACKOFFICE;
    }

    @GetMapping
    public List<StaffUser> list(@RequestParam(required = false) String sort,
                                @RequestParam(required = false) String direction) {
        return repository.findAll(sort, direction);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public StaffUser create(@Valid @RequestBody CreateStaff body, Authentication authentication) {
        return adminService.create(body.username(), body.password(), body.displayName(), body.role(),
                authentication.getName());
    }

    @PatchMapping("/{staffId}")
    @PreAuthorize("hasRole('ADMIN')")
    public StaffUser update(@PathVariable UUID staffId, @Valid @RequestBody UpdateStaff body,
                            Authentication authentication) {
        return adminService.update(staffId, body.displayName(), body.role(), authentication.getName());
    }

    @PostMapping("/{staffId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public StaffUser changeStatus(@PathVariable UUID staffId, @RequestBody ChangeStatus body,
                                  Authentication authentication) {
        return adminService.setStatus(staffId, body.active(), authentication.getName());
    }

    @PostMapping("/{staffId}/password-reset")
    @PreAuthorize("hasRole('ADMIN')")
    public void resetPassword(@PathVariable UUID staffId, @Valid @RequestBody ResetPassword body,
                              Authentication authentication) {
        adminService.resetPassword(staffId, body.password(), authentication.getName());
    }

    @PostMapping("/{staffId}/pin")
    public StaffUser setPin(@PathVariable UUID staffId, @Valid @RequestBody SetPin request) {
        return authService.setPin(staffId, request.pin());
    }

    @PostMapping("/{staffId}/stores")
    public List<UUID> bindStore(@PathVariable UUID staffId, @Valid @RequestBody BindStore request,
                                Authentication authentication) {
        authService.bindStore(staffId, request.storeId(), authentication.getName());
        return authService.storeIds(staffId);
    }

    @PostMapping("/{staffId}/stores/unbind")
    public List<UUID> unbindStore(@PathVariable UUID staffId, @Valid @RequestBody BindStore request,
                                  Authentication authentication) {
        authService.unbindStore(staffId, request.storeId(), authentication.getName());
        return authService.storeIds(staffId);
    }

    @GetMapping("/{staffId}/stores")
    public List<UUID> stores(@PathVariable UUID staffId) {
        return authService.storeIds(staffId);
    }
}
