ALTER TABLE orders
    ADD COLUMN referrer_staff_id UUID,
    ADD COLUMN referral_mode VARCHAR(16);

CREATE TABLE referral_binding (
    id UUID PRIMARY KEY,
    customer_phone VARCHAR(32) NOT NULL,
    promoter_staff_id UUID NOT NULL,
    mode VARCHAR(16) NOT NULL,
    source VARCHAR(32),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ
);

CREATE INDEX idx_referral_binding_phone ON referral_binding (customer_phone, active, created_at DESC);

CREATE TABLE distribution_commission (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    promoter_staff_id UUID NOT NULL,
    order_amount BIGINT NOT NULL,
    commission_amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    settled_at TIMESTAMPTZ,
    UNIQUE (order_id, promoter_staff_id)
);

CREATE INDEX idx_distribution_commission_promoter ON distribution_commission (promoter_staff_id, created_at DESC);