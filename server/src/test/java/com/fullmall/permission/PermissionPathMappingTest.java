package com.fullmall.permission;

import com.fullmall.permission.app.PermissionService;
import com.fullmall.permission.infra.PermissionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** 提现审核走独立权限，不再依赖写死角色；分销其它接口仍用促销权限。 */
class PermissionPathMappingTest {

    private final PermissionService service =
            new PermissionService(mock(PermissionRepository.class));

    @Test
    void withdrawalEndpointsRequireReviewPermission() {
        assertThat(service.requiredPermission("/admin/v1/distribution/withdrawals"))
                .isEqualTo("withdrawal.review");
        assertThat(service.requiredPermission("/admin/v1/distribution/withdrawals/abc/approve"))
                .isEqualTo("withdrawal.review");
        assertThat(service.requiredPermission("/admin/v1/distribution/withdrawals/abc/pay"))
                .isEqualTo("withdrawal.review");
    }

    @Test
    void otherDistributionEndpointsKeepPromotionPermission() {
        assertThat(service.requiredPermission("/admin/v1/distribution/promoters/x/upline"))
                .isEqualTo("promotion.manage");
        assertThat(service.requiredPermission("/admin/v1/distribution/commissions"))
                .isEqualTo("promotion.manage");
    }
}
