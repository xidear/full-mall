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

    public AddressService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Address(UUID id, String recipient, String phone, String province, String city,
                          String district, String detail, String postalCode, boolean isDefault) {
    }

    public record AddressView(UUID id, String recipient, String phoneMasked, String province, String city,
                              String district, String detail, String postalCode, boolean isDefault) {
    }

    public record AddressInput(String recipient, String phone, String regionCode, String province,
                               String city, String district, String detail, String postalCode) {
    }

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
                INSERT INTO member_address (id, owner_key, recipient, phone, region_code, province,
                                            city, district, detail, postal_code, is_default)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, ownerKey, input.recipient().trim(), input.phone().trim(), input.regionCode(),
                input.province(), input.city(), input.district(), input.detail().trim(),
                input.postalCode(), makeDefault);
        return id;
    }

    @Transactional
    public void update(String ownerKey, UUID id, AddressInput input) {
        requireOwner(ownerKey);
        validate(input);
        int updated = jdbcTemplate.update("""
                UPDATE member_address SET recipient = ?, phone = ?, region_code = ?, province = ?,
                       city = ?, district = ?, detail = ?, postal_code = ?, updated_at = now()
                 WHERE id = ? AND owner_key = ? AND deleted_at IS NULL
                """, input.recipient().trim(), input.phone().trim(), input.regionCode(),
                input.province(), input.city(), input.district(), input.detail().trim(),
                input.postalCode(), id, ownerKey);
        if (updated != 1) {
            throw ApiException.notFound("ADDRESS_NOT_FOUND", "地址不存在: " + id);
        }
    }

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

    public List<AddressView> list(String ownerKey) {
        requireOwner(ownerKey);
        return jdbcTemplate.query("""
                SELECT id, recipient, phone, province, city, district, detail, postal_code, is_default
                  FROM member_address
                 WHERE owner_key = ? AND deleted_at IS NULL
                 ORDER BY is_default DESC, created_at
                """, (rs, rowNum) -> new AddressView(
                rs.getObject("id", UUID.class), rs.getString("recipient"),
                FieldMasker.phone(rs.getString("phone")), rs.getString("province"),
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
                SELECT id, recipient, phone, province, city, district, detail, postal_code, is_default
                  FROM member_address
                 WHERE id = ? AND owner_key = ? AND deleted_at IS NULL
                """, (rs, rowNum) -> new Address(
                rs.getObject("id", UUID.class), rs.getString("recipient"), rs.getString("phone"),
                rs.getString("province"), rs.getString("city"), rs.getString("district"),
                rs.getString("detail"), rs.getString("postal_code"), rs.getBoolean("is_default")),
                id, ownerKey).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("ADDRESS_NOT_FOUND", "地址不存在: " + id));
    }

    private void validate(AddressInput input) {
        if (input.recipient() == null || input.recipient().isBlank()) {
            throw ApiException.badRequest("RECIPIENT_REQUIRED", "收件人不能为空");
        }
        if (input.phone() == null || !input.phone().trim().matches("^\\+?[0-9]{6,20}$")) {
            throw ApiException.badRequest("INVALID_PHONE", "手机号格式不正确");
        }
        if (input.detail() == null || input.detail().isBlank()) {
            throw ApiException.badRequest("ADDRESS_DETAIL_REQUIRED", "详细地址不能为空");
        }
    }

    private void requireOwner(String ownerKey) {
        if (ownerKey == null || ownerKey.isBlank()) {
            throw ApiException.badRequest("ADDRESS_OWNER_REQUIRED", "缺少地址归属标识");
        }
    }
}
