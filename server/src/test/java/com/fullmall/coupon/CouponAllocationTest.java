package com.fullmall.coupon;

import com.fullmall.coupon.app.CouponAllocationService;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 券分摊：按金额比例、总和严格等于券额、余数给最大行、非法输入拒绝。 */
class CouponAllocationTest {

    @Test
    void allocatesProportionallyAndKeepsSum() {
        Map<UUID, Long> lines = new LinkedHashMap<>();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        lines.put(a, 1000L);
        lines.put(b, 3000L);
        lines.put(c, 6000L);

        var allocations = CouponAllocationService.allocate(lines, 5000);

        long sum = allocations.stream().mapToLong(CouponAllocationService.Allocation::amount).sum();
        assertThat(sum).as("分摊之和必须等于券额").isEqualTo(5000);
        // 1000/10000*5000=500, 3000/10000*5000=1500, 6000/10000*5000=3000
        assertThat(allocations).anySatisfy(item -> {
            if (item.orderLineId().equals(a)) assertThat(item.amount()).isEqualTo(500);
        });
        assertThat(allocations).anySatisfy(item -> {
            if (item.orderLineId().equals(b)) assertThat(item.amount()).isEqualTo(1500);
        });
        assertThat(allocations).anySatisfy(item -> {
            if (item.orderLineId().equals(c)) assertThat(item.amount()).isEqualTo(3000);
        });
    }

    @Test
    void roundingRemainderGoesToLargestLine() {
        Map<UUID, Long> lines = new LinkedHashMap<>();
        UUID small = UUID.randomUUID();
        UUID big = UUID.randomUUID();
        lines.put(small, 1L);
        lines.put(big, 2L);

        var allocations = CouponAllocationService.allocate(lines, 2);

        assertThat(allocations.stream().mapToLong(CouponAllocationService.Allocation::amount).sum())
                .isEqualTo(2);
        assertThat(allocations).anySatisfy(item -> {
            if (item.orderLineId().equals(big)) assertThat(item.amount()).isEqualTo(2);
        });
    }

    @Test
    void invalidInputsAreRejected() {
        assertThatThrownBy(() -> CouponAllocationService.allocate(Map.of(), 100))
                .hasMessageContaining("没有可分摊的订单行");
        assertThatThrownBy(() -> CouponAllocationService.allocate(Map.of(UUID.randomUUID(), 0L), 100))
                .hasMessageContaining("必须大于 0");
        assertThatThrownBy(() -> CouponAllocationService.allocate(
                Map.of(UUID.randomUUID(), 100L), 200))
                .hasMessageContaining("超过订单商品金额合计");
    }
}
