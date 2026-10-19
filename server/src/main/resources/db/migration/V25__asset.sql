-- 素材库（P7 能力的一部分，作为 BaseEntity/Scope/BaseCrudController 的范式试点）
CREATE TABLE asset (
    id             UUID PRIMARY KEY,
    version        BIGINT      NOT NULL DEFAULT 0,
    name           VARCHAR(200) NOT NULL,
    url            VARCHAR(1024) NOT NULL,
    kind           VARCHAR(32)  NOT NULL,
    owner_username VARCHAR(64)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     UUID,
    updated_by     UUID,
    deleted_at     TIMESTAMPTZ
);

CREATE INDEX asset_owner_idx ON asset (owner_username, deleted_at);
CREATE INDEX asset_kind_idx ON asset (kind);