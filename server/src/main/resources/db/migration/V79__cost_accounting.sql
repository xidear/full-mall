-- 成本核算：SKU 级移动加权平均 + 成本流水（可追溯）
CREATE TABLE sku_cost (
    sku_id           UUID PRIMARY KEY REFERENCES sku(id),
    quantity         BIGINT NOT NULL DEFAULT 0,
    amount           BIGINT NOT NULL DEFAULT 0,
    avg_cost_amount  BIGINT NOT NULL DEFAULT 0,
    stale            BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT sku_cost_non_negative CHECK (quantity >= 0 AND amount >= 0 AND avg_cost_amount >= 0)
);

CREATE TABLE stock_cost_movement (
    id             UUID PRIMARY KEY,
    sku_id         UUID NOT NULL REFERENCES sku(id),
    movement_type  VARCHAR(16) NOT NULL,
    quantity       BIGINT NOT NULL,
    unit_cost      BIGINT NOT NULL,
    amount         BIGINT NOT NULL,
    reference_type VARCHAR(32),
    reference_id   VARCHAR(64),
    occurred_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT stock_cost_movement_type_check
        CHECK (movement_type IN ('RECEIPT', 'ISSUE', 'RETURN', 'ADJUST', 'WRITE_OFF')),
    CONSTRAINT stock_cost_movement_quantity_check CHECK (quantity <> 0)
);

CREATE INDEX stock_cost_movement_sku_idx ON stock_cost_movement (sku_id, occurred_at DESC);
