package com.fullmall.address.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.export.FieldMasker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 地址簿。
 *
 * <p>规则：
 * <ul>
 *   <li>第一个地址自动成为默认；</li>
 *   <li>同一归属人**最多一个默认**（DB 部分唯一索引兜底）；</li>
 *   <li>删除默认地址时若还有其他地址，必须先把另一个设为默认——**不静默改写用户的选择**；</li>
 *   <li>列表里的手机号做掩码（PII），详情返回完整值（本人使用）。</li>
 * </ul>
 */
@Service
public class AddressService {

    private final JdbcTemplate jdbcTemplate;
    /** 按国别判定必填字段：国家不同要填的字段不同（ADR-0013 §8），规则只从 address_format 读。 */
    private final com.fullmall.region.app.RegionService regions;

    public AddressService(JdbcTemplate jdbcTemplate, com.fullmall.region.app.RegionService regions) {
        this.jdbcTemplate = jdbcTemplate;
        this.regions = regions;
    }

    public record Address(UUID id, String recipient, String phone, String countryCode, String province,
                          String city, String district, String detail, String postalCode, boolean isDefault) {
    }

    public record AddressView(UUID id, String recipient, String phoneMasked, String countryCode,
                              String province, String city, String district, String detail,
                              String postalCode, boolean isDefault) {
    }

    public record AddressInput(String recipient, String phone, String countryCode, String regionCode,
                               String province, String city, String district, String detail,
                               String postalCode) {
    }

    /**
     * 新增地址；名下第一条自动成为默认。用 COUNT 判断“是否首条”而不是查 is_default：
     * 默认地址可能已被删光，只查 is_default 会留下“没有默认地址”的名单。并发新增由
     * member_address_default_idx 部分唯一索引兜底报错，不静默接受两个默认。
     */
    @Transactional
    public UUID create(String ownerKey, AddressInput input) {
        requireOwner(ownerKey);
        validate(input);
        Long existing = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM member_address WHERE owner_key = ? AND deleted_at IS NULL
                """, Long.class, ownerKey);
        boolean makeDefault = existing == null || existing == 0;

        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO member_address (id, owner_key, recipient, phone, country_code, region_code,
                                            province, city, district, detail, postal_code, is_default)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, ownerKey, input.recipient().trim(), input.phone().trim(),
                input.countryCode().toUpperCase(java.util.Locale.ROOT), input.regionCode(),
                input.province(), input.city(), input.district(), input.detail().trim(),
                input.postalCode(), makeDefault);
        return id;
    }

    @Transactional
    public void update(String ownerKey, UUID id, AddressInput input) {
        requireOwner(ownerKey);
        validate(input);
        int updated = jdbcTemplate.update("""
                UPDATE member_address SET recipient = ?, phone = ?, country_code = ?, region_code = ?,
                       province = ?, city = ?, district = ?, detail = ?, postal_code = ?,
                       updated_at = now()
                 WHERE id = ? AND owner_key = ? AND deleted_at IS NULL
                """, input.recipient().trim(), input.phone().trim(),
                input.countryCode().toUpperCase(java.util.Locale.ROOT), input.regionCode(),
                input.province(), input.city(), input.district(), input.detail().trim(),
                input.postalCode(), id, ownerKey);
        if (updated != 1) {
            throw ApiException.notFound("ADDRESS_NOT_FOUND", "地址不存在: " + id);
        }
    }

    /**
     * 设为默认：同事务、且必须“先清旧默认再置新默认”，否则会短暂出现两个默认而撞
     * member_address_default_idx。requireOwned 同时收紧归属，按猜 UUID 改别人地址簿返回 404。
     */
    @Transactional
    public void setDefault(String ownerKey, UUID id) {
        requireOwner(ownerKey);
        requireOwned(ownerKey, id);
        jdbcTemplate.update("""
                UPDATE member_address SET is_default = FALSE
                 WHERE owner_key = ? AND deleted_at IS NULL AND is_default AND id <> ?
                """, ownerKey, id);
        jdbcTemplate.update(
                "UPDATE member_address SET is_default = TRUE, updated_at = now() WHERE id = ?", id);
    }

    /**
     * 软删除（置 deleted_at 并清默认标记，释放部分唯一索引名额）。删的是默认地址且名下还有别的地址时拒绝：
     * 让系统替用户挑新默认，等于未经同意改发货地址。
     */
    @Transactional
    public void delete(String ownerKey, UUID id) {
        requireOwner(ownerKey);
        Address address = requireOwned(ownerKey, id);
        if (address.isDefault()) {
            Long others = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM member_address
                     WHERE owner_key = ? AND deleted_at IS NULL AND id <> ?
                    """, Long.class, ownerKey, id);
            if (others != null && others > 0) {
                throw ApiException.conflict("DEFAULT_ADDRESS_IN_USE",
                        "这是默认地址且还有其他地址，请先把另一个地址设为默认再删除");
            }
        }
        jdbcTemplate.update("""
                UPDATE member_address SET deleted_at = now(), is_default = FALSE WHERE id = ?
                """, id);
    }

    /** 列表手机号掩码（PII），只返回未删除地址，默认地址排最前。 */
    public List<AddressView> list(String ownerKey) {
        requireOwner(ownerKey);
        return jdbcTemplate.query("""
                SELECT id, recipient, phone, country_code, province, city, district, detail,
                       postal_code, is_default
                  FROM member_address
                 WHERE owner_key = ? AND deleted_at IS NULL
                 ORDER BY is_default DESC, created_at
                """, (rs, rowNum) -> new AddressView(
                rs.getObject("id", UUID.class), rs.getString("recipient"),
                FieldMasker.phone(rs.getString("phone")), rs.getString("country_code"),
                rs.getString("province"),
                rs.getString("city"), rs.getString("district"), rs.getString("detail"),
                rs.getString("postal_code"), rs.getBoolean("is_default")), ownerKey);
    }

    /** 详情返回完整手机号：只有本人能取到自己的地址。 */
    public Address get(String ownerKey, UUID id) {
        requireOwner(ownerKey);
        return requireOwned(ownerKey, id);
    }

    private Address requireOwned(String ownerKey, UUID id) {
        return jdbcTemplate.query("""
                SELECT id, recipient, phone, country_code, province, city, district, detail,
                       postal_code, is_default
                  FROM member_address
                 WHERE id = ? AND owner_key = ? AND deleted_at IS NULL
                """, (rs, rowNum) -> new Address(
                rs.getObject("id", UUID.class), rs.getString("recipient"), rs.getString("phone"),
                rs.getString("country_code"), rs.getString("province"), rs.getString("city"),
                rs.getString("district"),
                rs.getString("detail"), rs.getString("postal_code"), rs.getBoolean("is_default")),
                id, ownerKey).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("ADDRESS_NOT_FOUND", "地址不存在: " + id));
    }

    private void validate(AddressInput input) {
        if (input.recipient() == null || input.recipient().isBlank()) {
            throw ApiException.badRequest("RECIPIENT_REQUIRED", "收件人不能为空");
        }
        if (!com.fullmall.platform.validation.PhoneNumber.isValid(input.phone())) {
            throw ApiException.badRequest("INVALID_PHONE", "手机号格式不正确");
        }
        if (input.detail() == null || input.detail().isBlank()) {
            throw ApiException.badRequest("ADDRESS_DETAIL_REQUIRED", "详细地址不能为空");
        }
        // 国家是 Destination 轴：没有它就无法确定地址格式、运费、税与报关要素（ADR-0014）
        if (input.countryCode() == null || input.countryCode().isBlank()) {
            throw ApiException.badRequest("ADDRESS_COUNTRY_REQUIRED", "收货国家不能为空");
        }
        if (!input.countryCode().matches(
                com.fullmall.platform.validation.ValidationRules.ADDRESS_COUNTRY_PATTERN)) {
            throw ApiException.badRequest("INVALID_ADDRESS_COUNTRY",
                    "收货国家必须是 ISO 3166-1 alpha-2，实际=" + input.countryCode());
        }
        // 长度上限与列宽同源（§9.1）：超长在这里就拒绝，别等到写库才报错
        if (input.detail().length() > com.fullmall.platform.validation.ValidationRules.ADDRESS_DETAIL_MAX_LENGTH) {
            throw ApiException.badRequest("ADDRESS_DETAIL_TOO_LONG",
                    "详细地址最多 " + com.fullmall.platform.validation.ValidationRules.ADDRESS_DETAIL_MAX_LENGTH + " 个字符");
        }
        requireCountryFormat(input);
        requireRegionNameLength(input.province(), "province");
        requireRegionNameLength(input.city(), "city");
        requireRegionNameLength(input.district(), "district");
        if (input.regionCode() != null && !input.regionCode().isBlank()
                && !input.regionCode().matches(
                    com.fullmall.platform.validation.ValidationRules.ADDRESS_REGION_CODE_PATTERN)) {
            throw ApiException.badRequest("INVALID_REGION_CODE",
                    "区划码格式不正确，实际=" + input.regionCode());
        }
        if (input.postalCode() != null && input.postalCode().length()
                > com.fullmall.platform.validation.ValidationRules.ADDRESS_POSTAL_CODE_MAX_LENGTH) {
            throw ApiException.badRequest("ADDRESS_POSTAL_CODE_TOO_LONG",
                    "邮编最多 " + com.fullmall.platform.validation.ValidationRules.ADDRESS_POSTAL_CODE_MAX_LENGTH + " 个字符");
        }
    }

    /**
     * 按该国 address_format 校验必填项与邮编格式。
     *
     * <p>没有该国的格式就**拒绝**：套中国模板会让"日本用户的地址"看起来合法，
     * 但实际漏了都道府县，货发不出去（这是最典型的静默兜底）。
     */
    private void requireCountryFormat(AddressInput input) {
        String country = input.countryCode().toUpperCase(java.util.Locale.ROOT);
        com.fullmall.region.domain.AddressFormat format = regions.requireFormat(country);
        if (format.requires("S") && isBlank(input.province()) && isBlank(input.regionCode())) {
            throw ApiException.badRequest("ADDRESS_REGION_REQUIRED",
                    "该国家要求填写一级行政区（" + format.stateNameType() + "）");
        }
        // 有下级区划列表时（如中国省/市），城市也是必填；没有列表的国家（如新加坡）不要求
        if (format.requires("C") && isBlank(input.city()) && isBlank(input.regionCode())) {
            throw ApiException.badRequest("ADDRESS_CITY_REQUIRED", "该国家要求填写城市");
        }
        if (format.requires("Z") && isBlank(input.postalCode())) {
            throw ApiException.badRequest("ADDRESS_POSTAL_REQUIRED", "该国家要求填写邮政编码");
        }
        if (!isBlank(input.postalCode()) && format.zip() != null) {
            if (!input.postalCode().trim().matches("^(?:" + format.zip() + ")$")) {
                throw ApiException.badRequest("INVALID_POSTAL_CODE",
                        "邮政编码格式不符合该国家规则，实际=" + input.postalCode());
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void requireRegionNameLength(String value, String field) {
        if (value != null && value.length()
                > com.fullmall.platform.validation.ValidationRules.ADDRESS_REGION_NAME_MAX_LENGTH) {
            throw ApiException.badRequest("ADDRESS_REGION_NAME_TOO_LONG",
                    field + " 最多 "
                            + com.fullmall.platform.validation.ValidationRules.ADDRESS_REGION_NAME_MAX_LENGTH
                            + " 个字符");
        }
    }

    private void requireOwner(String ownerKey) {
        if (ownerKey == null || ownerKey.isBlank()) {
            throw ApiException.badRequest("ADDRESS_OWNER_REQUIRED", "缺少地址归属标识");
        }
    }
}
