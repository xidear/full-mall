-- 提现单：申请 → 审核 → 打款；审核通过后由打款消耗 AVAILABLE 佣金
CREATE TABLE withdrawal (
    id            UUID PRIMARY KEY,
    promoter_id   UUID NOT NULL,
    amount_amount BIGINT NOT NULL,
    currency      CHAR(3) NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    requested_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by   VARCHAR(128),
    reviewed_at   TIMESTAMPTZ,
    reject_reason VARCHAR(255),
    paid_at       TIMESTAMPTZ,
    payment_ref   VARCHAR(128),
    CONSTRAINT withdrawal_amount_positive CHECK (amount_amount > 0),
    CONSTRAINT withdrawal_status_check
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'PAID', 'FAILED'))
);

CREATE INDEX withdrawal_promoter_idx ON withdrawal (promoter_id, requested_at DESC);
CREATE INDEX withdrawal_status_idx ON withdrawal (status, requested_at);
