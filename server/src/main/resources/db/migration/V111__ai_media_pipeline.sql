-- AI 素材生产（图/视频/文案）：多供应商、异步任务、提示词与导图资产化
CREATE TABLE ai_provider (
    id          UUID PRIMARY KEY,
    kind        VARCHAR(16) NOT NULL,
    code        VARCHAR(48) NOT NULL UNIQUE,
    name        VARCHAR(96) NOT NULL,
    protocol    VARCHAR(32) NOT NULL,
    endpoint    VARCHAR(500) NOT NULL,
    api_key     TEXT,
    model       VARCHAR(96) NOT NULL,
    extra       JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    priority    INT NOT NULL DEFAULT 100,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ai_provider_kind_check CHECK (kind IN ('IMAGE', 'VIDEO', 'TEXT')),
    CONSTRAINT ai_provider_protocol_check
        CHECK (protocol IN ('openai-images', 'dashscope-image', 'dashscope-video', 'openai-chat'))
);

CREATE INDEX ai_provider_pick_idx ON ai_provider (kind, enabled, priority);

-- 生成任务（异步）：输出直接登记到素材库
CREATE TABLE ai_gen_job (
    id            UUID PRIMARY KEY,
    kind          VARCHAR(16) NOT NULL,
    provider_id   UUID NOT NULL REFERENCES ai_provider(id),
    product_id    UUID REFERENCES product(id),
    sku_id        UUID REFERENCES sku(id),
    purpose       VARCHAR(24) NOT NULL,
    prompt        TEXT NOT NULL,
    negative      TEXT,
    params        JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_asset_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts      INT NOT NULL DEFAULT 0,
    error         TEXT,
    asset_id      UUID,
    created_by    VARCHAR(64),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ai_gen_job_kind_check CHECK (kind IN ('IMAGE', 'VIDEO', 'TEXT')),
    CONSTRAINT ai_gen_job_purpose_check
        CHECK (purpose IN ('MAIN', 'SPEC', 'DETAIL', 'VIDEO', 'COPY')),
    CONSTRAINT ai_gen_job_status_check
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX ai_gen_job_queue_idx ON ai_gen_job (status, created_at);
CREATE INDEX ai_gen_job_product_idx ON ai_gen_job (product_id, created_at DESC);

-- 提示词历史（可复用、可优化）
CREATE TABLE ai_prompt_history (
    id          UUID PRIMARY KEY,
    kind        VARCHAR(16) NOT NULL,
    purpose     VARCHAR(24) NOT NULL,
    product_id  UUID,
    sku_id      UUID,
    prompt      TEXT NOT NULL,
    negative    TEXT,
    provider_code VARCHAR(48),
    asset_id    UUID,
    created_by  VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ai_prompt_history_scope_idx
    ON ai_prompt_history (product_id, sku_id, created_at DESC);

COMMENT ON TABLE ai_provider IS 'AI 供应商（同类可多家，按 priority 选择；使用时也可显式指定）';
COMMENT ON TABLE ai_gen_job IS '异步生成任务；结果落到资产库后回填 asset_id';
COMMENT ON TABLE ai_prompt_history IS '历史提示词，可直接上屏复用以减少重复输入';
