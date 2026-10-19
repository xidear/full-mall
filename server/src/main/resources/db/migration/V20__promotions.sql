CREATE TABLE promotion (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    type VARCHAR(24) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    sku_id UUID NOT NULL REFERENCES sku(id),
    promo_price_amount BIGINT,
    gift_sku_id UUID REFERENCES sku(id),
    buy_quantity INT,
    gift_quantity INT,
    max_gift_per_order INT,
    stock_limit INT,
    sold_quantity INT NOT NULL DEFAULT 0,
    per_user_limit INT,
    group_size INT,
    group_duration_hours INT,
    priority INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT promotion_type_check CHECK (type IN ('FLASH_SALE', 'BUY_GIFT', 'GROUP_BUY')),
    CONSTRAINT promotion_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'ENDED')),
    CONSTRAINT promotion_window_check CHECK (ends_at > starts_at),
    CONSTRAINT promotion_flash_check CHECK (
        type <> 'FLASH_SALE' OR (promo_price_amount IS NOT NULL AND promo_price_amount > 0
                                 AND stock_limit IS NOT NULL AND stock_limit > 0)
    ),
    CONSTRAINT promotion_gift_check CHECK (
        type <> 'BUY_GIFT' OR (gift_sku_id IS NOT NULL AND buy_quantity IS NOT NULL AND buy_quantity > 0
                               AND gift_quantity IS NOT NULL AND gift_quantity > 0)
    ),
    CONSTRAINT promotion_group_check CHECK (
        type <> 'GROUP_BUY' OR (promo_price_amount IS NOT NULL AND promo_price_amount > 0
                                AND group_size IS NOT NULL AND group_size >= 2
                                AND group_duration_hours IS NOT NULL AND group_duration_hours > 0)
    )
);

CREATE INDEX promotion_active_idx ON promotion (type, status, starts_at, ends_at);

CREATE TABLE promotion_redemption (
    id UUID PRIMARY KEY,
    promotion_id UUID NOT NULL REFERENCES promotion(id),
    order_id UUID NOT NULL REFERENCES orders(id),
    member_phone VARCHAR(32) NOT NULL,
    sale_quantity INT NOT NULL DEFAULT 0,
    gift_quantity INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT promotion_redemption_order_unique UNIQUE (promotion_id, order_id)
);

CREATE INDEX promotion_redemption_phone_idx ON promotion_redemption (promotion_id, member_phone);

CREATE TABLE group_buy (
    id UUID PRIMARY KEY,
    code VARCHAR(16) NOT NULL UNIQUE,
    promotion_id UUID NOT NULL REFERENCES promotion(id),
    sku_id UUID NOT NULL REFERENCES sku(id),
    price_amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    group_size INT NOT NULL,
    creator_phone VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'FORMING',
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT group_buy_status_check CHECK (status IN ('FORMING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX group_buy_status_idx ON group_buy (status, expires_at);

CREATE TABLE group_buy_member (
    id UUID PRIMARY KEY,
    group_buy_id UUID NOT NULL REFERENCES group_buy(id),
    order_id UUID NOT NULL REFERENCES orders(id),
    phone VARCHAR(32) NOT NULL,
    role VARCHAR(16) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    CONSTRAINT group_buy_member_role_check CHECK (role IN ('CREATOR', 'MEMBER')),
    CONSTRAINT group_buy_member_phone_unique UNIQUE (group_buy_id, phone),
    CONSTRAINT group_buy_member_order_unique UNIQUE (order_id)
);

ALTER TABLE order_line ADD COLUMN promotion_id UUID;
ALTER TABLE order_line ADD COLUMN line_type VARCHAR(16) NOT NULL DEFAULT 'NORMAL';
ALTER TABLE order_line ADD CONSTRAINT order_line_type_check CHECK (line_type IN ('NORMAL', 'PROMOTION', 'GIFT'));

INSERT INTO permission (code, label, group_name, sort_order)
VALUES ('promotion.manage', '促销与分销', '营销', 65)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role, permission_code)
VALUES ('OPERATOR', 'promotion.manage')
ON CONFLICT (role, permission_code) DO NOTHING;