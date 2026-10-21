-- 供应商售后（商城 → 供应商）：与商城售后单（顾客 → 商城）是两套流程，不共用状态机
CREATE TABLE supplier_return (
    id           UUID PRIMARY KEY,
    supplier_id  UUID NOT NULL,
    order_id     UUID NOT NULL REFERENCES orders(id),
    reason       VARCHAR(255) NOT NULL,
    amount_amount BIGINT NOT NULL,
    currency     CHAR(3) NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'REQUESTED',
    note         VARCHAR(512),
    created_by   VARCHAR(128) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at    TIMESTAMPTZ,
    CONSTRAINT supplier_return_status_check
        CHECK (status IN ('REQUESTED', 'ACCEPTED', 'REJECTED', 'SETTLED')),
    CONSTRAINT supplier_return_amount_check CHECK (amount_amount >= 0)
);

CREATE INDEX supplier_return_supplier_idx ON supplier_return (supplier_id, created_at DESC);
CREATE INDEX supplier_return_status_idx ON supplier_return (status, created_at DESC);
