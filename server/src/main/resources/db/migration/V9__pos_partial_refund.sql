ALTER TABLE pos_sale
    ADD COLUMN points_reversed BIGINT NOT NULL DEFAULT 0;

ALTER TABLE pos_sale_line
    ADD COLUMN refunded_amount BIGINT NOT NULL DEFAULT 0;

ALTER TABLE pos_sale_allocation
    ADD COLUMN refunded_quantity INT NOT NULL DEFAULT 0;

ALTER TABLE pos_tender
    ADD COLUMN refunded_amount BIGINT NOT NULL DEFAULT 0;

ALTER TABLE pos_refund
    ADD COLUMN refund_type VARCHAR(16) NOT NULL DEFAULT 'FULL';

CREATE TABLE pos_refund_line (
    id UUID PRIMARY KEY,
    refund_id UUID NOT NULL REFERENCES pos_refund(id),
    sale_line_id UUID NOT NULL REFERENCES pos_sale_line(id),
    quantity INT NOT NULL,
    amount_amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pos_refund_line_quantity_positive CHECK (quantity > 0),
    CONSTRAINT pos_refund_line_amount_positive CHECK (amount_amount >= 0)
);

CREATE INDEX idx_pos_refund_line_refund ON pos_refund_line (refund_id);
CREATE INDEX idx_pos_refund_line_sale_line ON pos_refund_line (sale_line_id);

CREATE TABLE pos_refund_allocation (
    id UUID PRIMARY KEY,
    refund_line_id UUID NOT NULL REFERENCES pos_refund_line(id),
    batch_id UUID NOT NULL REFERENCES inventory_batch(id),
    quantity INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pos_refund_allocation_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_pos_refund_allocation_line ON pos_refund_allocation (refund_line_id);

ALTER TABLE pos_sale_line
    ADD CONSTRAINT pos_sale_line_refunded_quantity_valid
        CHECK (refunded_quantity >= 0 AND refunded_quantity <= quantity);

ALTER TABLE pos_sale_line
    ADD CONSTRAINT pos_sale_line_refunded_amount_valid
        CHECK (refunded_amount >= 0 AND refunded_amount <= line_total_amount);

ALTER TABLE pos_sale_allocation
    ADD CONSTRAINT pos_sale_allocation_refunded_quantity_valid
        CHECK (refunded_quantity >= 0 AND refunded_quantity <= quantity);

ALTER TABLE pos_tender
    ADD CONSTRAINT pos_tender_refunded_amount_valid
        CHECK (refunded_amount >= 0 AND refunded_amount <= amount_amount);