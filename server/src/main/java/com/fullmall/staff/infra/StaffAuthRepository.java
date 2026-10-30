package com.fullmall.staff.infra;

import com.fullmall.staff.domain.StaffAccount;
import com.fullmall.staff.domain.StaffSessionUser;
import com.fullmall.staff.domain.StaffUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class StaffAuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public StaffAuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public StaffAccount findAccountByUsername(String username) {
        List<StaffAccount> accounts = jdbcTemplate.query("""
                SELECT id, username, password_hash, display_name, role, status
                FROM staff_user
                WHERE username = ?
                """, (rs, rowNum) -> new StaffAccount(
                new StaffUser(
                        rs.getObject("id", UUID.class),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getString("role"),
                        rs.getString("status")),
                rs.getString("password_hash")), username);
        return accounts.stream().findFirst().orElse(null);
    }

    /** 排序白名单：默认按创建时间。 */
    private static String orderByOf(String sort, String direction) {
        String column = switch (sort == null ? "" : sort.trim()) {
            case "displayName" -> "display_name";
            case "username" -> "username";
            case "role" -> "role";
            case "status" -> "status";
            default -> "created_at";
        };
        String dir = "asc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "ASC" : "DESC";
        return column + " " + dir;
    }

    public List<StaffUser> findAll() {
        return findAll(null, null);
    }

    public List<StaffUser> findAll(String sort, String direction) {
        return jdbcTemplate.query("""
                SELECT id, username, display_name, role, status
                FROM staff_user
                ORDER BY
                """ + orderByOf(sort, direction) + """
                """, (rs, rowNum) -> new StaffUser(
                rs.getObject("id", UUID.class),
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getString("role"),
                rs.getString("status")));
    }

    public StaffAccount findAccountById(UUID staffId) {
        List<StaffAccount> accounts = jdbcTemplate.query("""
                SELECT id, username, password_hash, display_name, role, status
                FROM staff_user
                WHERE id = ?
                """, (rs, rowNum) -> new StaffAccount(
                new StaffUser(
                        rs.getObject("id", UUID.class),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getString("role"),
                        rs.getString("status")),
                rs.getString("password_hash")), staffId);
        return accounts.stream().findFirst().orElse(null);
    }

    public void insertSession(String tokenHash, UUID staffId, Instant expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO staff_session (token_hash, staff_id, expires_at, created_at)
                VALUES (?, ?, ?, now())
                """, tokenHash, staffId, java.sql.Timestamp.from(expiresAt));
    }

    public StaffSessionUser findSession(String tokenHash) {
        List<StaffSessionUser> sessions = jdbcTemplate.query("""
                SELECT u.id, u.username, u.display_name, u.role, u.status, s.expires_at
                FROM staff_session s
                JOIN staff_user u ON u.id = s.staff_id
                WHERE s.token_hash = ?
                """, (rs, rowNum) -> new StaffSessionUser(
                new StaffUser(
                        rs.getObject("id", UUID.class),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getString("role"),
                        rs.getString("status")),
                rs.getTimestamp("expires_at").toInstant()), tokenHash);
        return sessions.stream().findFirst().orElse(null);
    }

    public void deleteSession(String tokenHash) {
        jdbcTemplate.update("DELETE FROM staff_session WHERE token_hash = ?", tokenHash);
    }

    public void deleteSessionsForStaff(UUID staffId) {
        jdbcTemplate.update("DELETE FROM staff_session WHERE staff_id = ?", staffId);
    }

    public void updatePin(UUID staffId, String pinHash) {
        int updated = jdbcTemplate.update("""
                UPDATE staff_user SET pin_hash = ? WHERE id = ?
                """, pinHash, staffId);
        if (updated != 1) {
            throw new IllegalStateException("Staff user not found: " + staffId);
        }
    }

    public String findPinHash(UUID staffId) {
        List<String> hashes = jdbcTemplate.query("""
                SELECT pin_hash FROM staff_user WHERE id = ?
                """, (rs, rowNum) -> rs.getString("pin_hash"), staffId);
        return hashes.isEmpty() ? null : hashes.get(0);
    }

    public void bindStore(UUID staffId, UUID storeId) {
        jdbcTemplate.update("""
                INSERT INTO staff_store (staff_id, store_id) VALUES (?, ?)
                ON CONFLICT (staff_id, store_id) DO NOTHING
                """, staffId, storeId);
    }

    public void unbindStore(UUID staffId, UUID storeId) {
        jdbcTemplate.update("DELETE FROM staff_store WHERE staff_id = ? AND store_id = ?", staffId, storeId);
    }

    public List<UUID> findStoreIds(UUID staffId) {
        return jdbcTemplate.query("""
                SELECT store_id FROM staff_store WHERE staff_id = ? ORDER BY assigned_at
                """, (rs, rowNum) -> rs.getObject("store_id", UUID.class), staffId);
    }

    public void updatePassword(UUID staffId, String passwordHash) {
        int updated = jdbcTemplate.update("""
                UPDATE staff_user SET password_hash = ?, updated_at = now() WHERE id = ?
                """, passwordHash, staffId);
        if (updated != 1) {
            throw new IllegalStateException("Staff user not found: " + staffId);
        }
    }

    public boolean usernameExists(String username) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM staff_user WHERE username = ?)", Boolean.class, username);
        return Boolean.TRUE.equals(exists);
    }

    public StaffUser insert(String username, String passwordHash, String displayName, String role) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO staff_user (id, username, password_hash, display_name, role, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', now(), now())
                """, id, username, passwordHash, displayName, role);
        return new StaffUser(id, username, displayName, role, "ACTIVE");
    }

    /** 只改档案字段（姓名/角色）；密码、PIN、门店各有专门入口，避免一次请求混着改敏感项。 */
    public StaffUser updateProfile(UUID staffId, String displayName, String role) {
        int updated = jdbcTemplate.update("""
                UPDATE staff_user SET display_name = ?, role = ?, updated_at = now() WHERE id = ?
                """, displayName, role, staffId);
        if (updated != 1) {
            throw new IllegalStateException("Staff user not found: " + staffId);
        }
        StaffAccount account = findAccountById(staffId);
        if (account == null) {
            throw new IllegalStateException("Staff user not found after update: " + staffId);
        }
        return account.user();
    }

    /** 生效中的管理员数量：用于拦住"停用最后一个管理员"把自己锁在门外。 */
    public int countActiveAdmins() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM staff_user WHERE role = 'ADMIN' AND status = 'ACTIVE'", Integer.class);
        return count == null ? 0 : count;
    }

    public StaffUser setStatus(UUID staffId, String status) {
        int updated = jdbcTemplate.update("""
                UPDATE staff_user SET status = ?, updated_at = now() WHERE id = ?
                """, status, staffId);
        if (updated != 1) {
            throw new IllegalStateException("Staff user not found: " + staffId);
        }
        StaffAccount account = findAccountById(staffId);
        if (account == null) {
            throw new IllegalStateException("Staff user not found after status change: " + staffId);
        }
        return account.user();
    }
}