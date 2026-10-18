CREATE TABLE idempotency_record (
    id UUID PRIMARY KEY,
    scope VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_order_id UUID,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (scope, idempotency_key)
);

CREATE INDEX idx_idempotency_record_created ON idempotency_record (created_at);