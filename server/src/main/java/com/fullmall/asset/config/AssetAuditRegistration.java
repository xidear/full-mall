package com.fullmall.asset.config;

import com.fullmall.platform.audit.AuditTypeRegistry;
import org.springframework.stereotype.Component;

/** 资产模块把自己的实体登记为"可查审计类型"（platform 不反向依赖业务模块）。 */
@Component
public class AssetAuditRegistration {

    public AssetAuditRegistration(AuditTypeRegistry registry,
                                  com.fullmall.platform.audit.AuditEntityLoaders loaders,
                                  com.fullmall.asset.infra.AssetRepository repository) {
        registry.register("asset", com.fullmall.asset.domain.AssetEntity.class);
        // 崩溃恢复按 id 重载当前实体，避免反序列化旧 payload 失败
        loaders.registerClass(com.fullmall.asset.domain.AssetEntity.class, "asset",
                id -> repository.findById(id).map(e -> (Object) e));
    }
}
