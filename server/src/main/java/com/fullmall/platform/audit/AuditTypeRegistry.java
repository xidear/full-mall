package com.fullmall.platform.audit;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 可查询审计的实体类型登记表。
 *
 * <p>为什么由模块**自行注册**：platform 不允许依赖业务模块（Modulith 边界），
 * 由 asset 等模块在自己的配置类里调用 {@link #register}，platform 只保留注册结果。
 *
 * <p>注册制同时也是一个白名单：未注册的类型无法被查询，避免任意类名反射。
 */
@Component
public class AuditTypeRegistry {

    private final Map<String, Class<?>> types = new LinkedHashMap<>();

    public void register(String name, Class<?> type) {
        types.put(name.trim().toLowerCase(Locale.ROOT), type);
    }

    public Class<?> find(String name) {
        return name == null ? null : types.get(name.trim().toLowerCase(Locale.ROOT));
    }

    public Set<String> names() {
        return types.keySet();
    }
}
