CREATE TABLE member (
    id UUID PRIMARY KEY,
    member_no VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NOT NULL UNIQUE,
    tier VARCHAR(16) NOT NULL DEFAULT 'STANDARD',
    points_balance BIGINT NOT NULL DEFAULT 0,
    stored_value_balance BIGINT NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT member_points_non_negative CHECK (points_balance >= 0),
    CONSTRAINT member_stored_value_non_negative CHECK (stored_value_balance >= 0)
);

CREATE TABLE points_ledger (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL REFERENCES member(id),
    sale_id UUID,
    entry_type VARCHAR(16) NOT NULL,
    points BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    reason VARCHAR(255),
    business_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_points_ledger_member ON points_ledger (member_id, created_at DESC);

CREATE TABLE member_card_ledger (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL REFERENCES member(id),
    sale_id UUID,
    entry_type VARCHAR(16) NOT NULL,
    amount_amount BIGINT NOT NULL,
    balance_after_amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    reason VARCHAR(255),
    business_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_member_card_ledger_member ON member_card_ledger (member_id, created_at DESC);

CREATE TABLE coupon (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    discount_type VARCHAR(16) NOT NULL,
    discount_value BIGINT NOT NULL,
    min_spend_amount BIGINT NOT NULL DEFAULT 0,
    max_discount_amount BIGINT,
    member_id UUID REFERENCES member(id),
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    status VARCHAR(16) NOT NULL DEFAULT 'ISSUED',
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ,
    used_at TIMESTAMPTZ,
    sale_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT coupon_discount_value_positive CHECK (discount_value > 0)
);

CREATE INDEX idx_coupon_member_status ON coupon (member_id, status, valid_to);

CREATE TABLE coupon_redemption (
    id UUID PRIMARY KEY,
    coupon_id UUID NOT NULL REFERENCES coupon(id),
    sale_id UUID NOT NULL,
    amount_amount BIGINT NOT NULL,
    operator_name VARCHAR(64),
    rolled_back_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_coupon_redemption_sale ON coupon_redemption (sale_id);

CREATE TABLE pos_shift (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES store(id),
    cashier_id UUID,
    cashier_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    currency CHAR(3) NOT NULL,
    opening_float_amount BIGINT NOT NULL DEFAULT 0,
    expected_cash_amount BIGINT,
    actual_cash_amount BIGINT,
    variance_amount BIGINT,
    variance_reason TEXT,
    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ,
    approved_by UUID,
    approved_at TIMESTAMPTZ
);

CREATE INDEX idx_pos_shift_store ON pos_shift (store_id, opened_at DESC);
CREATE UNIQUE INDEX uq_pos_shift_open
    ON pos_shift (store_id, cashier_name) WHERE status = 'OPEN';

CREATE TABLE pos_sale (
    id UUID PRIMARY KEY,
    sale_no VARCHAR(64) NOT NULL UNIQUE,
    store_id UUID NOT NULL REFERENCES store(id),
    shift_id UUID NOT NULL REFERENCES pos_shift(id),
    member_id UUID REFERENCES member(id),
    currency CHAR(3) NOT NULL,
    subtotal_amount BIGINT NOT NULL,
    promotion_discount_amount BIGINT NOT NULL DEFAULT 0,
    coupon_discount_amount BIGINT NOT NULL DEFAULT 0,
    member_discount_amount BIGINT NOT NULL DEFAULT 0,
    points_discount_amount BIGINT NOT NULL DEFAULT 0,
    payable_amount BIGINT NOT NULL,
    cash_tendered_amount BIGINT NOT NULL DEFAULT 0,
    change_amount BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL,
    coupon_id UUID REFERENCES coupon(id),
    points_redeemed BIGINT NOT NULL DEFAULT 0,
    points_earned BIGINT NOT NULL DEFAULT 0,
    refunded_amount BIGINT NOT NULL DEFAULT 0,
    note TEXT,
    idempotency_key VARCHAR(128),
    request_hash VARCHAR(64),
    operator_name VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at TIMESTAMPTZ,
    voided_at TIMESTAMPTZ,
    CONSTRAINT pos_sale_payable_non_negative CHECK (payable_amount >= 0)
);

CREATE INDEX idx_pos_sale_store_created ON pos_sale (store_id, created_at DESC);
CREATE INDEX idx_pos_sale_shift ON pos_sale (shift_id, created_at DESC);
CREATE UNIQUE INDEX uq_pos_sale_idempotency
    ON pos_sale (store_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE pos_sale_line (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES pos_sale(id),
    sku_id UUID NOT NULL,
    title_snapshot VARCHAR(255) NOT NULL,
    sku_code_snapshot VARCHAR(64) NOT NULL,
    quantity INT NOT NULL,
    unit_price_amount BIGINT NOT NULL,
    line_total_amount BIGINT NOT NULL,
    refunded_quantity INT NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL,
    CONSTRAINT pos_sale_line_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_pos_sale_line_sale ON pos_sale_line (sale_id);

CREATE TABLE pos_sale_allocation (
    id UUID PRIMARY KEY,
    sale_line_id UUID NOT NULL REFERENCES pos_sale_line(id),
    batch_id UUID NOT NULL REFERENCES inventory_batch(id),
    quantity INT NOT NULL,
    unit_cost_amount BIGINT,
    currency CHAR(3),
    CONSTRAINT pos_sale_allocation_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_pos_sale_allocation_line ON pos_sale_allocation (sale_line_id);

CREATE TABLE pos_tender (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES pos_sale(id),
    tender_type VARCHAR(16) NOT NULL,
    amount_amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_trade_no VARCHAR(128),
    status VARCHAR(16) NOT NULL,
    cash_tendered_amount BIGINT,
    change_amount BIGINT,
    idempotency_key VARCHAR(128),
    paid_at TIMESTAMPTZ,
    reversal_status VARCHAR(16),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pos_tender_amount_positive CHECK (amount_amount > 0)
);

CREATE INDEX idx_pos_tender_sale ON pos_tender (sale_id);
CREATE UNIQUE INDEX uq_pos_tender_idempotency
    ON pos_tender (sale_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE pos_cash_movement (
    id UUID PRIMARY KEY,
    shift_id UUID NOT NULL REFERENCES pos_shift(id),
    sale_id UUID,
    movement_type VARCHAR(24) NOT NULL,
    amount_amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    reason VARCHAR(255),
    operator_name VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pos_cash_movement_shift ON pos_cash_movement (shift_id, created_at);

CREATE TABLE pos_refund (
    id UUID PRIMARY KEY,
    refund_no VARCHAR(64) NOT NULL UNIQUE,
    sale_id UUID NOT NULL REFERENCES pos_sale(id),
    amount_amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    operator_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pos_refund_sale ON pos_refund (sale_id);

CREATE TABLE pos_refund_tender (
    id UUID PRIMARY KEY,
    refund_id UUID NOT NULL REFERENCES pos_refund(id),
    tender_id UUID REFERENCES pos_tender(id),
    tender_type VARCHAR(16) NOT NULL,
    amount_amount BIGINT NOT NULL,
    provider VARCHAR(32),
    provider_trade_no VARCHAR(128),
    status VARCHAR(16) NOT NULL
);

CREATE INDEX idx_pos_refund_tender_refund ON pos_refund_tender (refund_id);

CREATE TABLE pos_reconciliation (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES store(id),
    business_date DATE NOT NULL,
    tender_type VARCHAR(16) NOT NULL,
    expected_amount BIGINT NOT NULL,
    actual_amount BIGINT NOT NULL,
    variance_amount BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (store_id, business_date, tender_type)
);

INSERT INTO member (id, member_no, name, phone, tier, points_balance, stored_value_balance, currency)
VALUES
('a1000000-0000-0000-0000-000000000001', 'M0001', '王小明', '13800000001', 'GOLD', 500, 10000, 'CNY'),
('a1000000-0000-0000-0000-000000000002', 'M0002', '李静', '13800000002', 'STANDARD', 120, 0, 'CNY')
ON CONFLICT (id) DO NOTHING;

INSERT INTO coupon (id, code, name, discount_type, discount_value, min_spend_amount,
                    max_discount_amount, member_id, status, valid_from, valid_to)
VALUES
('c1000000-0000-0000-0000-000000000001', 'POS10', '满 100 元减 10 元', 'FIXED', 1000, 10000,
 NULL, NULL, 'ISSUED', now() - interval '1 day', now() + interval '365 days'),
('c1000000-0000-0000-0000-000000000002', 'MEMBER20', '会员 8 折券（最高减 30 元）', 'PERCENT', 20, 0,
 3000, 'a1000000-0000-0000-0000-000000000001', 'ISSUED', now() - interval '1 day', now() + interval '365 days'),
('c1000000-0000-0000-0000-000000000003', 'MILK5', '鲜奶立减 5 元', 'FIXED', 500, 0,
 NULL, NULL, 'ISSUED', now() - interval '1 day', now() + interval '365 days')
ON CONFLICT (id) DO NOTHING;