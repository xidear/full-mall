-- 支付对账（A2）：账单 vs 本地订单的差异记录
CREATE TABLE payment_reconciliation (
    id         UUID PRIMARY KEY,
    bill_date  DATE NOT NULL,
    order_no   VARCHAR(64) NOT NULL,
    kind       VARCHAR(24) NOT NULL,
    bill_fen   BIGINT,
    local_fen  BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT payment_reconciliation_kind_check
        CHECK (kind IN ('MATCHED', 'BILL_ONLY', 'LOCAL_ONLY', 'AMOUNT_MISMATCH')),
    CONSTRAINT payment_reconciliation_unique UNIQUE (bill_date, order_no)
);

CREATE INDEX payment_reconciliation_kind_idx ON payment_reconciliation (bill_date, kind);
