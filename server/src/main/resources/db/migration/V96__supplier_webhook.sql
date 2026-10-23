-- 供应商 Webhook（G5）：事件推送 + HMAC 签名 + 失败重试
CREATE TABLE supplier_webhook (
    id          UUID PRIMARY KEY,
    supplier_id UUID NOT NULL,
    url         VARCHAR(500) NOT NULL,
    secret      VARCHAR(128) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT supplier_webhook_url_check CHECK (url LIKE 'http%')
);

CREATE INDEX supplier_webhook_supplier_idx ON supplier_webhook (supplier_id) WHERE active;

CREATE TABLE webhook_delivery (
    id              UUID PRIMARY KEY,
    webhook_id      UUID NOT NULL REFERENCES supplier_webhook(id) ON DELETE CASCADE,
    event_type      VARCHAR(64) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts        INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT webhook_delivery_status_check
        CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX webhook_delivery_due_idx ON webhook_delivery (status, next_attempt_at);
