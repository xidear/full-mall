package com.fullmall.promotion;

import com.fullmall.promotion.app.SeckillGate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** C11：令牌不足 429、并发槽满 503、释放后可再进。 */
class SeckillGateTest {

    @Test
    void rejectsWhenTokensExhausted() {
        var gate = new SeckillGate(3, 0.001, 10);
        for (int i = 0; i < 3; i++) {
            gate.acquire("FLASH-1");
        }
        assertThatThrownBy(() -> gate.acquire("FLASH-1"))
                .hasMessageContaining("过于频繁");
    }

    @Test
    void refillsOverTime() throws Exception {
        var gate = new SeckillGate(1, 1000, 10);
        gate.acquire("FLASH-2");
        assertThatThrownBy(() -> gate.acquire("FLASH-2")).hasMessageContaining("过于频繁");
        Thread.sleep(20);
        gate.acquire("FLASH-2");
    }

    @Test
    void capsConcurrencyAndReleases() {
        var gate = new SeckillGate(100, 1000, 2);
        gate.enter("FLASH-3");
        gate.enter("FLASH-3");
        assertThat(gate.inFlight("FLASH-3")).isEqualTo(2);
        assertThatThrownBy(() -> gate.enter("FLASH-3")).hasMessageContaining("排队已满");
        gate.release("FLASH-3");
        gate.enter("FLASH-3");
        assertThat(gate.inFlight("FLASH-3")).isEqualTo(2);
    }
}
