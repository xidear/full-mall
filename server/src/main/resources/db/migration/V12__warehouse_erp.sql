CREATE TABLE goods_receipt (
    id UUID PRIMARY KEY,
    receipt_no VARCHAR(64) NOT NULL UNIQUE,
    location_type VARCHAR(16) NOT NULL,
    location_id UUID NOT NULL,
    supplier_id UUID REFERENCES supplier(id),
    status VARCHAR(16) NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at TIMESTAMPTZ
);

CREATE TABLE goods_receipt_line (
    id UUID PRIMARY KEY,
    receipt_id UUID NOT NULL REFERENCES goods_receipt(id),
    sku_id UUID NOT NULL REFERENCES sku(id),
    batch_no VARCHAR(64) NOT NULL,
    production_date DATE,
    expiry_date DATE,
    quantity INT NOT NULL,
    unit_cost_amount BIGINT,
    currency CHAR(3),
    CONSTRAINT goods_receipt_line_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_goods_receipt_line_receipt ON goods_receipt_line (receipt_id);

CREATE TABLE stocktake (
    id UUID PRIMARY KEY,
    stocktake_no VARCHAR(64) NOT NULL UNIQUE,
    location_type VARCHAR(16) NOT NULL,
    location_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at TIMESTAMPTZ
);

CREATE TABLE stocktake_line (
    id UUID PRIMARY KEY,
    stocktake_id UUID NOT NULL REFERENCES stocktake(id),
    batch_id UUID NOT NULL REFERENCES inventory_batch(id),
    sku_id UUID NOT NULL REFERENCES sku(id),
    system_quantity INT NOT NULL,
    counted_quantity INT,
    variance INT,
    reason VARCHAR(255)
);

CREATE INDEX idx_stocktake_line_stocktake ON stocktake_line (stocktake_id);

CREATE TABLE stock_transfer (
    id UUID PRIMARY KEY,
    transfer_no VARCHAR(64) NOT NULL UNIQUE,
    from_type VARCHAR(16) NOT NULL,
    from_id UUID NOT NULL,
    to_type VARCHAR(16) NOT NULL,
    to_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    shipped_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ
);

CREATE TABLE stock_transfer_line (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL REFERENCES stock_transfer(id),
    sku_id UUID NOT NULL REFERENCES sku(id),
    quantity INT NOT NULL,
    CONSTRAINT stock_transfer_line_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_stock_transfer_line_transfer ON stock_transfer_line (transfer_id);

CREATE TABLE stock_transfer_allocation (
    id UUID PRIMARY KEY,
    transfer_line_id UUID NOT NULL REFERENCES stock_transfer_line(id),
    source_batch_id UUID NOT NULL REFERENCES inventory_batch(id),
    batch_no VARCHAR(64) NOT NULL,
    production_date DATE,
    expiry_date DATE,
    quantity INT NOT NULL,
    unit_cost_amount BIGINT,
    currency CHAR(3),
    CONSTRAINT stock_transfer_allocation_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_stock_transfer_allocation_line ON stock_transfer_allocation (transfer_line_id);