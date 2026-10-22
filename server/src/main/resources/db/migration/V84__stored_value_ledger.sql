-- 储值流水：余额变动必须有账可查（此前只有余额字段，无法追溯）
CREATE TABLE stored_value_ledger (
    id            UUID PRIMARY KEY,
    member_id     UUID NOT NULL REFERENCES member(id),
    entry_type    VARCHAR(16) NOT NULL,
    amount        BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    business_key  VARCHAR(128) NOT NULL UNIQUE,
    note          VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT stored_value_ledger_type_check CHECK (entry_type IN ('TOPUP', 'CONSUME', 'REFUND', 'ADJUST')),
    CONSTRAINT stored_value_ledger_amount_check CHECK (amount <> 0),
    CONSTRAINT stored_value_ledger_balance_check CHECK (balance_after >= 0)
);

CREATE INDEX stored_value_ledger_member_idx ON stored_value_ledger (member_id, created_at DESC);
