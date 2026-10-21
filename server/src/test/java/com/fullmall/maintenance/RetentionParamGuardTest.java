package com.fullmall.maintenance;

import com.fullmall.platform.maintenance.api.MaintenanceController;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** 清理接口会真实删除数据：未知参数（如 dryRun）必须明确拒绝，不能被静默忽略。 */
class RetentionParamGuardTest {

    private final MaintenanceController controller =
            new MaintenanceController(mock(com.fullmall.platform.maintenance.MaintenanceService.class));

    @Test
    void rejectsUnknownParametersInsteadOfDeleting() {
        assertThatThrownBy(() -> controller.runRetention(null, null, null,
                Map.of("dryRun", "true")))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不支持参数")
                .hasMessageContaining("dryRun");
    }
}
