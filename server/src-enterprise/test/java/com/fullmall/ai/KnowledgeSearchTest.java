package com.fullmall.ai;

import com.fullmall.ai.app.InMemoryKnowledgeStore;
import com.fullmall.ai.app.KnowledgeChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 关键词检索：命中返回片段、无命中返回空（评测脚本依赖此语义）。 */
class KnowledgeSearchTest {

    @Test
    void searchReturnsMatchingChunksOnly() {
        var store = new InMemoryKnowledgeStore();
        store.replaceSource("PRODUCT", "sku-1", List.of(new KnowledgeChunk(
                UUID.randomUUID(), "PRODUCT", "sku-1", "有机鲜牛奶 · 价格与规格",
                "有机鲜牛奶 售价 19.90 元，保质期 7 天。", "v1")));

        assertThat(store.search("牛奶多少钱", 3)).hasSize(1);
        assertThat(store.search("牛奶多少钱", 3).get(0).title()).contains("鲜牛奶");
        assertThat(store.search("开发票", 3)).as("无命中必须返回空").isEmpty();
        // 同义词（鲜奶 vs 鲜牛奶）关键词检索召不回——这是向量检索要解决的问题，
        // 在评测集里必须如实计入未命中，不能靠"多写几个同义词"刷分。
        assertThat(store.search("鲜奶多少钱", 3)).isEmpty();
    }
}
