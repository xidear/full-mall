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

    /**
     * 新增地址；归属人名下第一条地址自动成为默认。
     *
     * <p>"是否首条"用 COUNT 判断而不是查 is_default：默认地址可能被删干净（只剩非默认），
     * 那种情况下再新增会留下"没有默认地址"的名单，结算时选不到地址。
     * 并发新增两条时两次都可能判定为首条，由 DB 的"每归属人最多一个默认"部分唯一索引兜底报错，
     * 不静默接受两个默认。
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

    /**
     * 设为默认：两步更新必须同事务，且顺序不能反。
     *
     * <p>先清旧默认再置新默认：反过来的话会短暂出现两个默认地址，撞上
     * {@code member_address_default_idx}（每个归属人最多一个未删除默认地址）直接失败。
     *
     * <p>{@code requireOwned} 同时收紧归属：id 不属于该 ownerKey 时按 404 处理，
     * 避免靠猜 UUID 改别人的地址簿。
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
     * 删除地址：软删除（置 {@code deleted_at} 并清默认标记，让部分唯一索引释放名额）。
     *
     * <p>删的是默认地址且名下还有别的地址时**拒绝**，要求用户先指定新默认：
     * 让系统替用户挑一个默认地址，等于在他没同意的情况下把货发到别处。
     * 只剩最后一条时允许删除（清空地址簿是用户的权利）。
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

    /**
     * 列表里的手机号做掩码（PII），且只返回未删除地址；默认地址排最前。
     *
     * <p>掩码只在列表做：详情 {@link #get} 返回完整号码，因为那是本人查看自己的地址——
     * 后台/导出场景若要展示号码，请走 {@code FieldMasker} 统一口径，而不是各自截字符串。
     */
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
        if (!com.fullmall.platform.validation.PhoneNumber.isValid(input.phone())) {
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
