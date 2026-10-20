package com.fullmall.asset.config;

import com.fullmall.platform.audit.AuditTypeRegistry;
import org.springframework.stereotype.Component;

/** 资产模块把自己的实体登记为"可查审计类型"（platform 不反向依赖业务模块）。 */
@Component
public class AssetAuditRegistration {

    public AssetAuditRegistration(AuditTypeRegistry registry) {
        registry.register("asset", com.fullmall.asset.domain.AssetEntity.class);
    }
}
