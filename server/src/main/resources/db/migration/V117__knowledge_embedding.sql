-- 语义检索索引（L9）：存向量，Java 侧算余弦（数据量小，避免引入 pgvector 扩展）
CREATE TABLE knowledge_embedding (
    id         UUID PRIMARY KEY,
    kind       VARCHAR(32) NOT NULL,
    source_id  VARCHAR(64) NOT NULL,
    content    TEXT NOT NULL,
    embedding  DOUBLE PRECISION[] NOT NULL,
    provider   VARCHAR(48) NOT NULL,
    model      VARCHAR(96) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT knowledge_embedding_unique UNIQUE (kind, source_id)
);

CREATE INDEX knowledge_embedding_kind_idx ON knowledge_embedding (kind, created_at DESC);
