package com.fullmall.member.export;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MemberExportRepository {

    private final JdbcTemplate jdbcTemplate;

    public MemberExportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record MemberRow(String memberNo, String name, String phone, String tier,
                            long pointsBalance, long storedValueBalance, String currency) {
    }

    public List<MemberRow> findForExport(int limit) {
        return jdbcTemplate.query("""
                SELECT member_no, name, phone, tier, points_balance, stored_value_balance, currency
                  FROM member ORDER BY member_no LIMIT ?
                """, (rs, rowNum) -> new MemberRow(
                rs.getString("member_no"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("tier"),
                rs.getLong("points_balance"),
                rs.getLong("stored_value_balance"),
                rs.getString("currency")), limit);
    }
}