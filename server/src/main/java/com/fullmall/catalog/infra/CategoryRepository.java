package com.fullmall.catalog.infra;

import com.fullmall.catalog.domain.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class CategoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public CategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Category> findActive() {
        return jdbcTemplate.query("""
                SELECT id, parent_id, code, name
                FROM category
                WHERE active = true
                ORDER BY code
                """, (rs, rowNum) -> new Category(
                rs.getObject("id", UUID.class),
                rs.getObject("parent_id", UUID.class),
                rs.getString("code"),
                rs.getString("name")));
    }
}