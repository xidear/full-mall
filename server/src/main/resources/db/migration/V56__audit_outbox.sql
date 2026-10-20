-- 审计 outbox：业务事务内落库，独立消费者写 JaVers，进程崩溃不丢审计
CREATE TABLE audit_outbox (
    id           UUID PRIMARY KEY,
    entity_type  VARCHAR(255) NOT NULL,
    entity_id    VARCHAR(128) NOT NULL,
    author       VARCHAR(128) NOT NULL,
    payload      JSONB NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts     INT NOT NULL DEFAULT 0,
    last_error   VARCHAR(500),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    CONSTRAINT audit_outbox_status_check CHECK (status IN ('PENDING', 'DONE', 'FAILED')),
    CONSTRAINT audit_outbox_attempts_non_negative CHECK (attempts >= 0)
);

CREATE INDEX audit_outbox_pending_idx ON audit_outbox (status, created_at);
