-- 地址簿（结算前置）：按归属人隔离，软删除，默认地址唯一
CREATE TABLE member_address (
    id          UUID PRIMARY KEY,
    owner_key   VARCHAR(128) NOT NULL,
    recipient   VARCHAR(64)  NOT NULL,
    phone       VARCHAR(32)  NOT NULL,
    region_code VARCHAR(16),
    province    VARCHAR(64),
    city        VARCHAR(64),
    district    VARCHAR(64),
    detail      VARCHAR(255) NOT NULL,
    postal_code VARCHAR(16),
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX member_address_owner_idx ON member_address (owner_key, deleted_at);
-- 每个归属人最多一个默认地址（未删除的）
CREATE UNIQUE INDEX member_address_default_idx
    ON member_address (owner_key) WHERE is_default AND deleted_at IS NULL;
