package com.fullmall.cart.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 服务端购物车（跨端一致）。
 *
 * <p>ownerKey 形如 `member:<id>` 或 `guest:<token>`；登录后调用 {@link #merge} 合并游客车。
 * 规则：
 * <ul>
 *   <li>同一 SKU 重复加购**累加数量**，不产生重复行；</li>
 *   <li>数量必须 > 0；把数量改成 0 视为非法请求（删除请显式调用 remove）；</li>
 *   <li>合并用 `quantity = 原 + 游客`，随后清空游客车。</li>
 * </ul>
 */
@Service
public class CartService {

    private final JdbcTemplate jdbcTemplate;

    public CartService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record CartItem(UUID id, UUID skuId, int quantity) {
    }

    @Transactional
    public UUID addItem(String ownerKey, UUID skuId, int quantity) {
        requireOwner(ownerKey);
        if (skuId == null) {
            throw ApiException.badRequest("SKU_REQUIRED", "必须指定商品 SKU");
        }
        if (quantity <= 0) {
            throw ApiException.badRequest("INVALID_QUANTITY", "数量必须大于 0，实际=" + quantity);
        }
        UUID cartId = ensureCart(ownerKey);
        int updated = jdbcTemplate.update("""
                INSERT INTO cart_item (id, cart_id, sku_id, quantity)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (cart_id, sku_id) DO UPDATE
                   SET quantity = cart_item.quantity + EXCLUDED.quantity, updated_at = now()
                """, UUID.randomUUID(), cartId, skuId, quantity);
        if (updated < 1) {
            throw new IllegalStateException("购物车写入失败: owner=" + ownerKey + ", sku=" + skuId);
        }
        touch(cartId);
        return cartId;
    }

    @Transactional
    public void updateQuantity(String ownerKey, UUID skuId, int quantity) {
        requireOwner(ownerKey);
        if (quantity <= 0) {
            throw ApiException.badRequest("INVALID_QUANTITY",
                    "数量必须大于 0；移除商品请调用删除接口");
        }
        UUID cartId = findCart(ownerKey);
        int updated = jdbcTemplate.update("""
                UPDATE cart_item SET quantity = ?, updated_at = now()
                 WHERE cart_id = ? AND sku_id = ?
                """, quantity, cartId, skuId);
        if (updated != 1) {
            throw ApiException.notFound("CART_ITEM_NOT_FOUND", "购物车中没有该商品: " + skuId);
        }
        touch(cartId);
    }

    @Transactional
    public void remove(String ownerKey, UUID skuId) {
        requireOwner(ownerKey);
        UUID cartId = findCart(ownerKey);
        int deleted = jdbcTemplate.update(
                "DELETE FROM cart_item WHERE cart_id = ? AND sku_id = ?", cartId, skuId);
        if (deleted != 1) {
            throw ApiException.notFound("CART_ITEM_NOT_FOUND", "购物车中没有该商品: " + skuId);
        }
        touch(cartId);
    }

    @Transactional
    public void clear(String ownerKey) {
        requireOwner(ownerKey);
        UUID cartId = findCart(ownerKey);
        jdbcTemplate.update("DELETE FROM cart_item WHERE cart_id = ?", cartId);
        touch(cartId);
    }

    public List<CartItem> items(String ownerKey) {
        requireOwner(ownerKey);
        return jdbcTemplate.query("""
                SELECT i.id, i.sku_id, i.quantity
                  FROM cart_item i JOIN cart c ON c.id = i.cart_id
                 WHERE c.owner_key = ?
                 ORDER BY i.added_at
                """, (rs, rowNum) -> new CartItem(
                rs.getObject("id", UUID.class), rs.getObject("sku_id", UUID.class), rs.getInt("quantity")),
                ownerKey);
    }

    /** 登录后把游客车合并到会员车：数量相加，然后清空游客车。 */
    @Transactional
    public int merge(String guestKey, String memberKey) {
        requireOwner(guestKey);
        requireOwner(memberKey);
        List<CartItem> guestItems = items(guestKey);
        if (guestItems.isEmpty()) {
            return 0;
        }
        for (CartItem item : guestItems) {
            addItem(memberKey, item.skuId(), item.quantity());
        }
        clear(guestKey);
        return guestItems.size();
    }

    private UUID ensureCart(String ownerKey) {
        List<UUID> existing = jdbcTemplate.queryForList(
                "SELECT id FROM cart WHERE owner_key = ?", UUID.class, ownerKey);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO cart (id, owner_key) VALUES (?, ?)", id, ownerKey);
        return id;
    }

    private UUID findCart(String ownerKey) {
        List<UUID> existing = jdbcTemplate.queryForList(
                "SELECT id FROM cart WHERE owner_key = ?", UUID.class, ownerKey);
        if (existing.isEmpty()) {
            throw ApiException.notFound("CART_NOT_FOUND", "购物车不存在: " + ownerKey);
        }
        return existing.get(0);
    }

    private void touch(UUID cartId) {
        jdbcTemplate.update("UPDATE cart SET updated_at = now() WHERE id = ?", cartId);
    }

    private void requireOwner(String ownerKey) {
        if (ownerKey == null || ownerKey.isBlank()) {
            throw ApiException.badRequest("CART_OWNER_REQUIRED", "缺少购物车归属标识");
        }
    }
}
