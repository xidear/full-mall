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

    public List<StaffUser> findAll() {
        return jdbcTemplate.query("""
                SELECT id, username, display_name, role, status
                FROM staff_user
                ORDER BY created_at
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

    public void updatePassword(UUID staffId, String passwordHash) {
        int updated = jdbcTemplate.update("""
                UPDATE staff_user SET password_hash = ? WHERE id = ?
                """, passwordHash, staffId);
        if (updated != 1) {
            throw new IllegalStateException("Staff user not found: " + staffId);
        }
    }
}