package com.fullmall.coupon.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 券额分摊（C4）：把整单券优惠**按订单行金额比例**摊到每一行。
 *
 * <p>为什么需要：部分退货时，系统必须知道"这一行用了多少券"，
 * 否则退款只能按整单比例拍脑袋，容易出现多退/少退。
 *
 * <p>取整规则：按比例向下取整，**余数给金额最大的那一行**，保证分摊之和 = 券额。
 */
@Service
public class CouponAllocationService {

    private final JdbcTemplate jdbc;

    public CouponAllocationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Allocation(UUID orderLineId, long amount) {
    }

    /**
     * 纯计算：给定各行金额与券总额，返回分摊结果（总和严格等于 couponDiscount）。
     */
    public static List<Allocation> allocate(Map<UUID, Long> lineAmounts, long couponDiscount) {
        if (lineAmounts == null || lineAmounts.isEmpty()) {
            throw ApiException.badRequest("COUPON_ALLOCATION_EMPTY", "没有可分摊的订单行");
        }
        long total = lineAmounts.values().stream().mapToLong(Long::longValue).sum();
        if (total <= 0) {
            throw ApiException.badRequest("COUPON_ALLOCATION_NO_AMOUNT", "订单行金额合计必须大于 0");
        }
        if (couponDiscount <= 0) {
            throw ApiException.badRequest("COUPON_ALLOCATION_INVALID", "券额必须大于 0");
        }
        if (couponDiscount > total) {
            throw ApiException.badRequest("COUPON_ALLOCATION_EXCEEDS",
                    "券额 " + couponDiscount + " 超过订单商品金额合计 " + total);
        }
        Map<UUID, Long> result = new LinkedHashMap<>();
        long assigned = 0;
        UUID largestLine = null;
        long largestAmount = -1;
        for (var entry : lineAmounts.entrySet()) {
            long share = couponDiscount * entry.getValue() / total;   // 向下取整
            result.put(entry.getKey(), share);
            assigned += share;
            if (entry.getValue() > largestAmount) {
                largestAmount = entry.getValue();
                largestLine = entry.getKey();
            }
        }
        long remainder = couponDiscount - assigned;
        if (remainder > 0) {
            result.put(largestLine, result.get(largestLine) + remainder);
        }
        List<Allocation> allocations = new ArrayList<>();
        result.forEach((lineId, amount) -> allocations.add(new Allocation(lineId, amount)));
        return allocations;
    }

    /** 落库：同一订单重复调用会先清空再写（幂等）。 */
    @Transactional
    public List<Allocation> apply(UUID orderId, UUID couponId, Map<UUID, Long> lineAmounts,
                                  long couponDiscount) {
        List<Allocation> allocations = allocate(lineAmounts, couponDiscount);
        jdbc.update("DELETE FROM order_coupon_allocation WHERE order_id = ?", orderId);
        for (Allocation allocation : allocations) {
            jdbc.update("""
                    INSERT INTO order_coupon_allocation (order_id, order_line_id, coupon_id, amount)
                    VALUES (?, ?, ?, ?)
                    """, orderId, allocation.orderLineId(), couponId, allocation.amount());
        }
        return allocations;
    }

    /** 查询某订单的分摊明细（售后计算用）。 */
    public List<Allocation> findAllocations(UUID orderId) {
        return jdbc.query("""
                SELECT order_line_id, amount FROM order_coupon_allocation
                 WHERE order_id = ? ORDER BY order_line_id
                """, (rs, rowNum) -> new Allocation(
                rs.getObject("order_line_id", UUID.class), rs.getLong("amount")), orderId);
    }
}
