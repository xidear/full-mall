-- 采购单与收货单：成本的唯一合法来源（不允许手填成本）
CREATE TABLE purchase_order (
    id            UUID PRIMARY KEY,
    order_no      VARCHAR(64) NOT NULL UNIQUE,
    supplier_id   UUID REFERENCES supplier(id),
    status        VARCHAR(16) NOT NULL,
    currency      CHAR(3) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    received_at   TIMESTAMPTZ,
    CONSTRAINT purchase_order_status_check CHECK (status IN ('DRAFT', 'PLACED', 'RECEIVED', 'CANCELLED'))
);

CREATE TABLE purchase_order_line (
    id             UUID PRIMARY KEY,
    purchase_id    UUID NOT NULL REFERENCES purchase_order(id),
    sku_id         UUID NOT NULL REFERENCES sku(id),
    quantity       BIGINT NOT NULL,
    unit_cost      BIGINT NOT NULL,
    received_qty   BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT purchase_line_quantity_check CHECK (quantity > 0),
    CONSTRAINT purchase_line_cost_check CHECK (unit_cost >= 0),
    CONSTRAINT purchase_line_received_check CHECK (received_qty >= 0 AND received_qty <= quantity)
);

CREATE INDEX purchase_order_line_purchase_idx ON purchase_order_line (purchase_id);
