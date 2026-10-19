-- 供应商认证模式与 Bearer Token（SUP-030）
-- auth_mode: HMAC_ONLY / TOKEN_ONLY / BOTH；历史凭证默认保持 HMAC_ONLY，行为不变。
ALTER TABLE supplier_credential
    ADD COLUMN auth_mode VARCHAR(16) NOT NULL DEFAULT 'HMAC_ONLY',
    ADD COLUMN token_hash VARCHAR(64),
    ADD COLUMN token_expires_at TIMESTAMPTZ,
    ADD COLUMN token_revoked_at TIMESTAMPTZ;

ALTER TABLE supplier_credential
    ADD CONSTRAINT supplier_credential_auth_mode_check
    CHECK (auth_mode IN ('HMAC_ONLY', 'TOKEN_ONLY', 'BOTH'));