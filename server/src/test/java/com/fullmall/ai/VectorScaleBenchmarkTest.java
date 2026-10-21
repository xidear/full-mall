package com.fullmall.ai;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 暴力余弦检索的规模基线（不是性能测试，只用于给出"什么时候必须上 ANN"的实测依据）。
 */
class VectorScaleBenchmarkTest {

    private static float[] randomVector(Random random, int dim) {
        float[] vector = new float[dim];
        for (int i = 0; i < dim; i++) {
            vector[i] = random.nextFloat() * 2 - 1;
        }
        return vector;
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    @Test
    void bruteForceCostAtHundredThousandChunks() {
        int dim = 256;          // 保守维度：真实 bge-m3 是 1024，耗时约 4 倍
        int chunks = 100_000;
        Random random = new Random(42);
        float[][] data = new float[chunks][];
        for (int i = 0; i < chunks; i++) {
            data[i] = randomVector(random, dim);
        }
        float[] query = randomVector(random, dim);

        for (int i = 0; i < 2000; i++) {
            cosine(query, data[i]);
        }

        long start = System.nanoTime();
        int queries = 20;
        for (int q = 0; q < queries; q++) {
            double best = -1;
            for (float[] row : data) {
                best = Math.max(best, cosine(query, row));
            }
            assertThat(best).isGreaterThan(-1.1);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        long perQuery = elapsedMs / queries;

        System.out.println("BENCH dim=" + dim + " chunks=" + chunks
                + " totalMs=" + elapsedMs + " perQueryMs=" + perQuery
                + " estDim1024PerQueryMs=" + (perQuery * 4));

        assertThat(perQuery).isLessThan(2_000);
    }
}
