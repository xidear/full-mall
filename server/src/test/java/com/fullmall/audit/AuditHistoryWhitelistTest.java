package com.fullmall.audit;

import com.fullmall.platform.audit.AuditTrail;
import com.fullmall.platform.audit.api.AuditHistoryController;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** 审计查询只允许白名单类型，避免任意类名反射。 */
class AuditHistoryWhitelistTest {

    private final com.fullmall.platform.audit.AuditTypeRegistry registry =
            new com.fullmall.platform.audit.AuditTypeRegistry();
    private final AuditHistoryController controller =
            new AuditHistoryController(mock(AuditTrail.class), registry);

    {
        registry.register("asset", Object.class);   // 模拟模块自注册
    }

    @Test
    void rejectsUnknownEntityType() {
        assertThatThrownBy(() -> controller.history("java.lang.Runtime", UUID.randomUUID()))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不支持查询该类型的审计记录");
    }

    @Test
    void acceptsWhitelistedAssetTypeCaseInsensitively() {
        // 白名单内类型不应抛"类型不允许"；AuditTrail 是 mock，返回空列表即可
        assertThat(controller.history("ASSET", UUID.randomUUID())).isEmpty();
    }
}
