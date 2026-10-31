package com.fullmall.ai.answer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 关键词打分必须是可解释的：完全包含 0.95，否则按二元组重合率。 */
class KeywordRetrieverTest {

    @Test
    void exactContainmentScoresHighest() {
        assertThat(KeywordRetriever.score(KeywordRetriever.normalize("满99免运费"),
                "满99免运费，偏远地区另计")).isEqualTo(0.95);
    }

    @Test
    void sharedBigramsProducePartialScore() {
        double score = KeywordRetriever.score(
                KeywordRetriever.normalize("运费怎么算"),
                "满 99 元免运费，偏远地区另计。");
        // 只共享“运费”一个二元组，4 个里中 1 个 = 0.25
        assertThat(score).isBetween(0.2, 0.3);
    }

    @Test
    void unrelatedTextScoresZero() {
        assertThat(KeywordRetriever.score(
                KeywordRetriever.normalize("退货政策"),
                "满 99 元免运费")).isZero();
    }

    @Test
    void punctuationAndSpacesAreIgnored() {
        assertThat(KeywordRetriever.score(
                KeywordRetriever.normalize("运 费，怎么算？"),
                "运费怎么算")).isEqualTo(0.95);
    }
}