CREATE TABLE supplier_credential (
    id UUID PRIMARY KEY,
    supplier_id UUID NOT NULL UNIQUE REFERENCES supplier(id),
    access_key VARCHAR(64) NOT NULL UNIQUE,
    secret_encrypted TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    rotated_at TIMESTAMPTZ
);

ALTER TABLE fulfillment_order
    ADD COLUMN supplier_accepted_at TIMESTAMPTZ,
    ADD COLUMN supplier_note TEXT;

CREATE TABLE supplier_stock_feed (
    id UUID PRIMARY KEY,
    supplier_id UUID NOT NULL REFERENCES supplier(id),
    sku_id UUID NOT NULL REFERENCES sku(id),
    supplier_sku_code VARCHAR(64) NOT NULL,
    available_quantity INT NOT NULL,
    price_amount BIGINT,
    price_currency CHAR(3),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (supplier_id, sku_id),
    CONSTRAINT supplier_stock_feed_quantity_non_negative CHECK (available_quantity >= 0)
);

CREATE INDEX idx_supplier_stock_feed_supplier ON supplier_stock_feed (supplier_id, updated_at DESC);
