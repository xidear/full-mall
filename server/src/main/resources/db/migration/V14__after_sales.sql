CREATE TABLE after_sale (
    id UUID PRIMARY KEY,
    after_sale_no VARCHAR(64) NOT NULL UNIQUE,
    order_id UUID NOT NULL REFERENCES orders(id),
    type VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    description TEXT,
    contact_phone VARCHAR(32) NOT NULL,
    refund_amount BIGINT NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL,
    admin_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ
);

CREATE INDEX idx_after_sale_order ON after_sale (order_id, created_at DESC);
CREATE INDEX idx_after_sale_status ON after_sale (status, created_at DESC);

CREATE TABLE after_sale_line (
    id UUID PRIMARY KEY,
    after_sale_id UUID NOT NULL REFERENCES after_sale(id),
    order_line_id UUID NOT NULL REFERENCES order_line(id),
    sku_id UUID NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT after_sale_line_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_after_sale_line_sale ON after_sale_line (after_sale_id);

CREATE TABLE after_sale_event (
    id UUID PRIMARY KEY,
    after_sale_id UUID NOT NULL REFERENCES after_sale(id),
    event_type VARCHAR(32) NOT NULL,
    note TEXT,
    operator_name VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_after_sale_event_sale ON after_sale_event (after_sale_id, created_at);