package com.fullmall.platform.audit.api;

import com.fullmall.platform.audit.AuditEventCatalog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计页的筛选候选：事件类型、操作人、实体类型。
 *
 * <p>三样都是**后端下发**，页面不再让运营手填：
 * 事件码看不懂、操作人靠猜拼写、实体名靠记忆，都是把校验成本推给用户。
 *
 * <p>事件类型取"目录 ∪ 库里实际出现过的码"：既保证常见事件有中文标签，
 * 也不会因为某条历史数据用了未登记的码而筛不到。
 */
@RestController
@RequestMapping("/admin/v1/audit")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class AuditMetaController {

    private final JdbcTemplate jdbc;

    public AuditMetaController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Actor(String value, String label) {
    }

    @GetMapping("/event-types")
    public List<AuditEventCatalog.Entry> eventTypes() {
        Map<String, AuditEventCatalog.Entry> merged = new LinkedHashMap<>();
        for (AuditEventCatalog.Entry entry : AuditEventCatalog.entries()) {
            merged.put(entry.code(), entry);
        }
        List<String> seen = jdbc.queryForList(
                "SELECT DISTINCT event_type FROM audit_event", String.class);
        for (String code : seen) {
            merged.computeIfAbsent(code, AuditEventCatalog::unknown);
        }
        return List.copyOf(merged.values());
    }

    @GetMapping("/actors")
    public List<Actor> actors() {
        List<String> names = jdbc.queryForList(
                "SELECT DISTINCT actor FROM audit_event WHERE actor IS NOT NULL ORDER BY actor", String.class);
        return names.stream().map(name -> new Actor(name, name)).toList();
    }

    @GetMapping("/resources")
    public List<String> resources() {
        return jdbc.queryForList(
                "SELECT DISTINCT resource FROM audit_event WHERE resource IS NOT NULL ORDER BY resource", String.class);
    }
}
