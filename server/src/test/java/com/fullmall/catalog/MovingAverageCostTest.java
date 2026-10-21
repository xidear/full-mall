package com.fullmall.catalog;

import com.fullmall.catalog.app.MovingAverageCost;
import com.fullmall.catalog.app.MovingAverageCost.State;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 成本核算 COST-1/COST-2：两次入库取加权平均；出库不改变均价。 */
class MovingAverageCostTest {

    @Test
    void twoReceiptsProduceWeightedAverage() {
        State afterFirst = MovingAverageCost.receive(MovingAverageCost.initial(), 10, 1_000);
        assertThat(afterFirst.avgCost()).isEqualTo(1_000);   // 10 元

        State afterSecond = MovingAverageCost.receive(afterFirst, 10, 1_400);
        assertThat(afterSecond.quantity()).isEqualTo(20);
        assertThat(afterSecond.amount()).isEqualTo(24_000);
        assertThat(afterSecond.avgCost()).isEqualTo(1_200);  // 12 元
        assertThat(afterSecond.stale()).isFalse();
    }

    @Test
    void issueKeepsAverageAndReducesAmount() {
        State state = MovingAverageCost.receive(MovingAverageCost.initial(), 20, 1_200);

        State afterIssue = MovingAverageCost.issue(state, 5);

        assertThat(afterIssue.quantity()).isEqualTo(15);
        assertThat(afterIssue.amount()).isEqualTo(18_000);
        assertThat(afterIssue.avgCost()).as("移动平均：出库不改变单位成本").isEqualTo(1_200);
    }

    @Test
    void sellingOutKeepsLastAverageButMarksStale() {
        State state = MovingAverageCost.receive(MovingAverageCost.initial(), 3, 1_333);

        State empty = MovingAverageCost.issue(state, 3);

        assertThat(empty.quantity()).isZero();
        assertThat(empty.amount()).isZero();
        assertThat(empty.avgCost()).isEqualTo(1_333);
        assertThat(empty.stale()).isTrue();
    }

    @Test
    void invalidInputsAreRejected() {
        assertThatThrownBy(() -> MovingAverageCost.receive(MovingAverageCost.initial(), 0, 100))
                .hasMessageContaining("入库数量必须大于 0");
        assertThatThrownBy(() -> MovingAverageCost.receive(MovingAverageCost.initial(), 1, -1))
                .hasMessageContaining("入库成本不能为负");
        assertThatThrownBy(() -> MovingAverageCost.issue(MovingAverageCost.initial(), 1))
                .hasMessageContaining("超过库存");
    }
}
