package com.fullmall.ai.app;

import java.util.List;

/** 检索端口：pgvector 实现由 infra 提供，测试可用内存实现。 */
public interface KnowledgeRetriever {

    /**
     * @param query 顾客问题
     * @param topK  返回条数上限（>0）
     * @return 按相关度降序；**不得返回空字符串内容**，无结果就是空列表
     */
    List<KnowledgeChunk> retrieve(String query, int topK);
}
