-- AI 知识块。embedding 以 float32 小端字节存储：
-- 避免依赖 pgvector 扩展（嵌入式测试库没有），召回先用关键词筛选再由应用做余弦重排。
-- 数据量超过约 10 万块时，应加 pgvector 与 HNSW 索引（见 docs/AI_SERVICE_MODULE.md §3）。
CREATE TABLE knowledge_chunk (
    id          UUID PRIMARY KEY,
    source_type VARCHAR(32) NOT NULL,
    source_id   VARCHAR(128) NOT NULL,
    title       VARCHAR(300) NOT NULL,
    content     TEXT NOT NULL,
    version     VARCHAR(64) NOT NULL,
    embedding   BYTEA,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX knowledge_chunk_source_idx ON knowledge_chunk (source_type, source_id);
CREATE INDEX knowledge_chunk_updated_idx ON knowledge_chunk (updated_at DESC);
