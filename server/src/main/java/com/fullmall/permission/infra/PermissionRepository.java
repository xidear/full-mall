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
