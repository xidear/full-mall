-- ADR-005：第三方 API 配置入库（敏感字段加密）
CREATE TABLE integration_config (
    capability  VARCHAR(32) NOT NULL,
    provider    VARCHAR(32) NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT false,
    config_enc  BYTEA NOT NULL,
    config_hint JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_by  VARCHAR(64),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (capability, provider)
);

-- 每类能力同一时间只有一个启用，避免行为不确定
CREATE UNIQUE INDEX integration_config_one_enabled
    ON integration_config (capability) WHERE enabled;
