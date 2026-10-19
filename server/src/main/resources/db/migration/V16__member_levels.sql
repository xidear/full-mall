ALTER TABLE member
    ADD COLUMN birthday DATE,
    ADD COLUMN growth_value BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN birthday_gift_year INT;

CREATE TABLE member_level (
    code VARCHAR(16) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    discount_rate INT NOT NULL,
    points_multiplier INT NOT NULL DEFAULT 1,
    free_shipping BOOLEAN NOT NULL DEFAULT FALSE,
    birthday_points BIGINT NOT NULL DEFAULT 0,
    min_growth BIGINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT member_level_discount_valid CHECK (discount_rate BETWEEN 1 AND 100)
);

INSERT INTO member_level (code, name, discount_rate, points_multiplier, free_shipping, birthday_points, min_growth, sort_order)
VALUES
('STANDARD', '普通会员', 100, 1, FALSE, 100, 0, 10),
('SILVER', '银卡会员', 98, 1, FALSE, 200, 1000, 20),
('GOLD', '金卡会员', 95, 2, TRUE, 500, 5000, 30),
('PLATINUM', '白金会员', 92, 2, TRUE, 1000, 20000, 40)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE growth_ledger (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL REFERENCES member(id),
    sale_id UUID,
    entry_type VARCHAR(16) NOT NULL,
    growth BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    reason VARCHAR(255),
    business_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_growth_ledger_member ON growth_ledger (member_id, created_at DESC);