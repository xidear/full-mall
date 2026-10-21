package com.fullmall.ai;

import com.fullmall.ai.app.VectorMath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 向量编码与余弦：维度不符/零向量必须返回 -1（=不参与排序），不能当成 0 分混进结果。 */
class VectorMathTest {

    @Test
    void roundTripsFloat32() {
        float[] original = {0.1f, -2.5f, 3.75f};
        float[] decoded = VectorMath.fromBytes(VectorMath.toBytes(original));

        assertThat(decoded).containsExactly(original);
    }

    @Test
    void cosineSimilarityBasics() {
        assertThat(VectorMath.cosine(new float[]{1, 0}, new float[]{1, 0})).isEqualTo(1.0);
        assertThat(VectorMath.cosine(new float[]{1, 0}, new float[]{0, 1})).isEqualTo(0.0);
        assertThat(VectorMath.cosine(new float[]{1, 0}, new float[]{-1, 0})).isEqualTo(-1.0);
    }

    @Test
    void mismatchedOrZeroVectorsAreRejected() {
        assertThat(VectorMath.cosine(new float[]{1, 0}, new float[]{1, 0, 0}))
                .as("维度不符").isEqualTo(-1);
        assertThat(VectorMath.cosine(new float[]{0, 0}, new float[]{1, 0}))
                .as("零向量无方向").isEqualTo(-1);
        assertThat(VectorMath.fromBytes(new byte[]{1, 2, 3}))
                .as("字节数不是 4 的倍数").isNull();
    }
}
