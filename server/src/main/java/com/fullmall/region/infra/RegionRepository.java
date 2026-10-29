package com.fullmall.region.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.region.domain.AddressFormat;
import com.fullmall.region.domain.RegionNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 区划字典与国别地址格式的只读访问（数据来自离线快照，运行期不访问外网）。 */
@Repository
public class RegionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public RegionRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<RegionNode> findChildren(String countryCode, String parentCode, String level) {
        StringBuilder sql = new StringBuilder("""
                SELECT code, parent_code, level, depth, country_code, names, active
                  FROM region
                 WHERE active
                """);
        List<Object> args = new java.util.ArrayList<>();
        // 查"国家清单"时 countryCode 为 null：不能写 country_code = ?（NULL 比较恒不成立，列表会永远为空），
        // 必须整段跳过国家过滤，只按 level=COUNTRY 取根节点。
        if (countryCode != null) {
            sql.append(" AND country_code = ?");
            args.add(countryCode);
        }
        if (parentCode != null) {
            sql.append(" AND parent_code = ?");
            args.add(parentCode);
        } else if ("COUNTRY".equals(level)) {
            sql.append(" AND parent_code IS NULL");
        } else {
            // 不给 parent 时返回"该国的直接下级"（一级行政区），而不是把国家节点和更深层级一起倒出来：
            // 否则前端拿到的列表里会混进国家和市级，级联下拉长度对不上。
            if (countryCode == null) {
                throw new IllegalStateException("查下级区划必须指定国家，否则会把所有国家的区划混在一起");
            }
            sql.append(" AND parent_code = ?");
            args.add(countryCode);
        }
        if (level != null) {
            sql.append(" AND level = ?");
            args.add(level);
        }
        sql.append(" ORDER BY depth, code");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new RegionNode(
                rs.getString("code"), rs.getString("parent_code"), rs.getString("level"),
                rs.getInt("depth"), rs.getString("country_code"), names(rs.getString("names")),
                rs.getBoolean("active")), args.toArray());
    }

    public Optional<RegionNode> findNode(String code) {
        return jdbcTemplate.query("""
                SELECT code, parent_code, level, depth, country_code, names, active
                  FROM region WHERE code = ?
                """, (rs, rowNum) -> new RegionNode(
                rs.getString("code"), rs.getString("parent_code"), rs.getString("level"),
                rs.getInt("depth"), rs.getString("country_code"), names(rs.getString("names")),
                rs.getBoolean("active")), code).stream().findFirst();
    }

    public Optional<AddressFormat> findFormat(String countryCode) {
        return jdbcTemplate.query("""
                SELECT country_code, required, fmt, zip, zipex, state_name_type,
                       has_admin_list, admin_optional, upper
                  FROM address_format WHERE country_code = ?
                """, (rs, rowNum) -> new AddressFormat(
                rs.getString("country_code"), rs.getString("required"), rs.getString("fmt"),
                rs.getString("zip"), rs.getString("zipex"), rs.getString("state_name_type"),
                rs.getBoolean("has_admin_list"), rs.getBoolean("admin_optional"),
                rs.getString("upper")), countryCode).stream().findFirst();
    }

    private Map<String, String> names(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("区划 names 不是合法的 locale→名称 JSON: " + json, exception);
        }
    }
}
