-- 供应商 nonce 持久化（SUP-032）：防重放必须跨重启、跨副本有效。
CREATE TABLE supplier_nonce (
    access_key VARCHAR(64)  NOT NULL,
    nonce      VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (access_key, nonce)
);

CREATE INDEX supplier_nonce_created_idx ON supplier_nonce (created_at);