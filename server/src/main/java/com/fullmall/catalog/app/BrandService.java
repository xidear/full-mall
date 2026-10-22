package com.fullmall.catalog.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** 品牌管理（V72 建表；此前只能靠 SQL 维护）。 */
@Service
public class BrandService {

    private final JdbcTemplate jdbc;

    public BrandService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Brand(UUID id, String code, String name, int sortOrder, long productCount) {
    }

    public List<Brand> list() {
        return jdbc.query("""
                SELECT b.id, b.code, b.name, b.sort_order,
                       (SELECT COUNT(*) FROM product p WHERE p.brand_id = b.id) AS product_count
                  FROM brand b ORDER BY b.sort_order, b.name
                """, (rs, rowNum) -> new Brand(rs.getObject("id", UUID.class), rs.getString("code"),
                rs.getString("name"), rs.getInt("sort_order"), rs.getLong("product_count")));
    }

    @Transactional
    public UUID create(String code, String name, int sortOrder) {
        validate(code, name);
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO brand (id, code, name, sort_order) VALUES (?, ?, ?, ?)",
                    id, code.trim(), name.trim(), sortOrder);
        } catch (org.springframework.dao.DuplicateKeyException duplicate) {
            throw ApiException.conflict("BRAND_CODE_DUPLICATED", "品牌编码已存在：" + code);
        }
        return id;
    }

    @Transactional
    public void update(UUID id, String name, int sortOrder) {
        if (name == null || name.isBlank()) {
            throw ApiException.badRequest("BRAND_NAME_REQUIRED", "品牌名称不能为空");
        }
        int updated = jdbc.update("UPDATE brand SET name = ?, sort_order = ? WHERE id = ?",
                name.trim(), sortOrder, id);
        if (updated != 1) {
            throw ApiException.notFound("BRAND_NOT_FOUND", "品牌不存在：" + id);
        }
    }

    /** 删除：被商品引用时拒绝（避免商品品牌悬空）。 */
    @Transactional
    public void delete(UUID id) {
        Long used = jdbc.queryForObject(
                "SELECT COUNT(*) FROM product WHERE brand_id = ?", Long.class, id);
        if (used != null && used > 0) {
            throw ApiException.conflict("BRAND_IN_USE",
                    "该品牌已被 " + used + " 个商品引用，不能删除");
        }
        int deleted = jdbc.update("DELETE FROM brand WHERE id = ?", id);
        if (deleted != 1) {
            throw ApiException.notFound("BRAND_NOT_FOUND", "品牌不存在：" + id);
        }
    }

    private void validate(String code, String name) {
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("BRAND_CODE_REQUIRED", "品牌编码不能为空");
        }
        if (name == null || name.isBlank()) {
            throw ApiException.badRequest("BRAND_NAME_REQUIRED", "品牌名称不能为空");
        }
    }
}
