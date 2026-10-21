package com.fullmall.ai.app;

import java.util.List;

/** 知识存储端口：演示用内存实现，生产用 pgvector 实现。 */
public interface KnowledgeStore {

    /** 按来源替换（同源先删后写，保证"最新参数"不会被旧知识污染）。 */
    void replaceSource(String sourceType, String sourceId, List<KnowledgeChunk> chunks);

    /** 关键词检索（演示模式）；生产实现应叠加向量召回。 */
    List<KnowledgeChunk> search(String query, int topK);

    int size();
}
