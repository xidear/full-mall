package com.fullmall.platform.identity;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 订单归属校验（跨模块共享，放在 platform 层避免 order ↔ fulfillment 循环依赖）。
 *
 * <p>规则：
 * <ul>
 *   <li>订单已绑定会员 → 调用者必须是该会员，否则一律 **404**（不暴露存在性）；</li>
 *   <li>游客订单 → 必须提供与订单联系人一致的手机号，否则 **403**。</li>
 * </ul>
 *
 * <p>为什么游客单也要求手机号：履约数据里含**自提码**，仅凭订单号就能拿到别人可取货的凭证是严重漏洞。
 */
@Service
public class OrderOwnershipGuard {

    private final JdbcTemplate jdbcTemplate;

    public OrderOwnershipGuard(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private record OrderOwner(UUID memberId, String contactPhone, String accessTokenHash) {
    }

    /** 游客订单令牌的哈希（明文只在下单响应里返回一次）。 */
    public static String hashToken(String token) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((token + ":fullmall-order").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 不可用", error);
        }
    }

    public void requireAccess(UUID orderId, UUID memberId, String phone) {
        requireAccess(orderId, memberId, phone, null);
    }

    /**
     * @param accessToken 游客订单令牌（下单时下发）；与库中哈希匹配即通过。
     *                    未绑定会员的订单，**令牌或手机号至少一个正确**才放行。
     */
    public void requireAccess(UUID orderId, UUID memberId, String phone, String accessToken) {
        List<OrderOwner> rows = jdbcTemplate.query("""
                SELECT member_id, contact_phone, access_token_hash FROM orders WHERE id = ?
                """, (rs, rowNum) -> new OrderOwner(
                rs.getObject("member_id", UUID.class), rs.getString("contact_phone"),
                rs.getString("access_token_hash")), orderId);

        if (rows.isEmpty()) {
            throw ApiException.notFound("ORDER_NOT_FOUND", "订单不存在: " + orderId);
        }
        OrderOwner owner = rows.get(0);

        if (owner.memberId() != null) {
            if (memberId == null || !owner.memberId().equals(memberId)) {
                throw ApiException.notFound("ORDER_NOT_FOUND", "订单不存在: " + orderId);
            }
            return;
        }

        boolean tokenMatches = accessToken != null && !accessToken.isBlank()
                && owner.accessTokenHash() != null
                && java.security.MessageDigest.isEqual(
                        hashToken(accessToken).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        owner.accessTokenHash().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (tokenMatches) {
            return;
        }

        if (phone == null || phone.isBlank() || !owner.contactPhone().equals(phone.trim())) {
            throw ApiException.conflict("ORDER_ACCESS_DENIED",
                    "需要订单联系人手机号才能查看该订单的履约信息");
        }
    }
}
