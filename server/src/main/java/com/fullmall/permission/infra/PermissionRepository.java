package com.fullmall.permission.infra;

import com.fullmall.permission.api.PermissionDtos.PermissionView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PermissionRepository {

    private final JdbcTemplate jdbcTemplate;

    public PermissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PermissionView> permissions() {
        return jdbcTemplate.query("""
                SELECT code, label, group_name, sort_order FROM permission
                ORDER BY sort_order, code
                """, (rs, rowNum) -> new PermissionView(
                rs.getString("code"), rs.getString("label"), rs.getString("group_name"),
                rs.getInt("sort_order")));
    }

    /** 可在矩阵里配置权限的角色（role.editable=true），从表里读而不是写死。 */
    public List<String> editableRoleCodes() {
        return jdbcTemplate.queryForList(
                "SELECT code FROM role WHERE editable ORDER BY sort_order, code", String.class);
    }

    /** 角色标签由角色表下发，前端不再维护一份 roleLabels。 */
    public List<java.util.Map<String, Object>> roles() {
        return jdbcTemplate.query("SELECT code, label, editable FROM role ORDER BY sort_order, code",
                (rs, rowNum) -> {
                    java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("role", rs.getString("code"));
                    row.put("label", rs.getString("label"));
                    row.put("editable", rs.getBoolean("editable"));
                    return row;
                });
    }

    public int countStaffWithRole(String role) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM staff_user WHERE role = ?", Integer.class, role);
        return count == null ? 0 : count;
    }

    public void deleteRole(String role) {
        jdbcTemplate.update("DELETE FROM role_permission WHERE role = ?", role);
        jdbcTemplate.update("DELETE FROM role WHERE code = ?", role);
    }

    public List<String> rolePermissions(String role) {
        return jdbcTemplate.query("""
                SELECT permission_code FROM role_permission
                WHERE role = ? ORDER BY permission_code
                """, (rs, rowNum) -> rs.getString("permission_code"), role);
    }

    public void replaceRolePermissions(String role, List<String> codes) {
        jdbcTemplate.update("DELETE FROM role_permission WHERE role = ?", role);
        for (String code : codes) {
            jdbcTemplate.update("""
                    INSERT INTO role_permission (role, permission_code) VALUES (?, ?)
                    ON CONFLICT (role, permission_code) DO NOTHING
                    """, role, code);
        }
    }

    public boolean permissionExists(String code) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM permission WHERE code = ?
                """, Integer.class, code);
        return count != null && count > 0;
    }
}
