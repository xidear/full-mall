package com.fullmall.platform.audit.api;

import com.fullmall.platform.audit.AuditTrail;
import com.fullmall.platform.error.ApiException;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 审计历史查询（运营/客服追溯用）。
 *
 * <p>**只开放白名单类型**：JaVers 按类名反射加载，若允许任意类名，等于给外部一个
 * 反射入口（可加载任意类并读取属性）。白名单显式列出允许查询的实体。
 *
 * <p>当前仅资产（`AssetEntity`）接入审计；其它模块尚未 commit，见 `docs/OPEN_ITEMS.md` I6。
 */
@RestController
@RequestMapping("/admin/v1/audit/history")
public class AuditHistoryController {

    private final AuditTrail auditTrail;
    private final com.fullmall.platform.audit.AuditTypeRegistry registry;

    public AuditHistoryController(AuditTrail auditTrail,
                                  com.fullmall.platform.audit.AuditTypeRegistry registry) {
        this.auditTrail = auditTrail;
        this.registry = registry;
    }

    @GetMapping
    public List<Map<String, Object>> history(@RequestParam String entityType,
                                             @RequestParam UUID entityId) {
        Class<?> type = registry.find(entityType);
        if (type == null) {
            throw ApiException.badRequest("AUDIT_TYPE_NOT_ALLOWED",
                    "不支持查询该类型的审计记录：" + entityType + "（允许：" + registry.names() + "）");
        }
        List<CdoSnapshot> snapshots = auditTrail.history(entityId, type);
        return snapshots.stream().map(snapshot -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("commitDate", snapshot.getCommitMetadata().getCommitDate().toString());
            row.put("author", snapshot.getCommitMetadata().getAuthor());
            row.put("type", snapshot.getType().name());
            row.put("changed", snapshot.getChanged());
            row.put("state", snapshot.getState());
            return row;
        }).toList();
    }
}
