package com.fullmall.platform.audit;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * 审计恢复用的"按 id 重载实体"注册表。
 *
 * <p>为什么需要它：崩溃恢复原本靠反序列化 payload 还原对象，实体字段/构造器变化后必然失败
 * （实测 `recoverOrphans` 从未成功提交过）。**从数据库按 id 重载**才是可靠做法：
 * 拿到的是当前真实实体，JaVers 能正常快照。
 *
 * <p>代价：恢复记录的是"当前状态"而非"崩溃时刻的状态"。对于审计补齐这是可接受的——
 * 至少不会静默丢失整条记录，且 payload 仍保留可人工比对。
 */
@Component
public class AuditEntityLoaders {

    private final Map<String, Function<UUID, Optional<Object>>> loaders = new LinkedHashMap<>();

    /** 按类型名注册（支持模块别名、简单类名或全类名，内部统一小写）。 */
    public void register(String entityType, Function<UUID, Optional<Object>> loader) {
        loaders.put(entityType.trim().toLowerCase(Locale.ROOT), loader);
    }

    /**
     * 按实体类注册：同时登记**全类名**与**简单类名**。
     *
     * <p>outbox 存的是全类名；模块别名（如 `asset`）与类名无关，必须分别登记，
     * 否则恢复时查不到加载器会静默退回反序列化并失败。
     */
    public void registerClass(Class<?> type, String alias, Function<UUID, Optional<Object>> loader) {
        register(type.getName(), loader);
        register(type.getSimpleName(), loader);
        if (alias != null && !alias.isBlank()) {
            register(alias, loader);
        }
    }

    /**
     * 按类型名查找加载器：**同时支持简短名与全类名**。
     *
     * <p>outbox 里存的是全类名（如 `com.fullmall.asset.domain.AssetEntity`），
     * 而模块注册时用的是简短名（`asset`）。只按全名查会查不到，静默退回反序列化路径并失败——
     * 这正是恢复长年不生效的原因。
     */
    public Function<UUID, Optional<Object>> find(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            return null;
        }
        String raw = entityType.trim();
        Function<UUID, Optional<Object>> direct = loaders.get(raw.toLowerCase(Locale.ROOT));
        if (direct != null) {
            return direct;
        }
        // 注意：先从原始字符串取简单类名再转小写，否则 "AssetEntity" 会变成 "assetentity"
        int dot = raw.lastIndexOf('.');
        String simple = dot >= 0 ? raw.substring(dot + 1) : raw;
        return loaders.get(simple.toLowerCase(Locale.ROOT));
    }

    public java.util.Set<String> keys() { return java.util.Set.copyOf(loaders.keySet()); }

    public boolean supports(String entityType) {
        return find(entityType) != null;
    }
}
