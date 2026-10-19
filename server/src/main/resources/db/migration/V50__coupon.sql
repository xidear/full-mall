-- 优惠券模板与会员券。抵扣接入结算待"促销叠加策略"确定，本迁移只承载券的生命周期。
CREATE TABLE coupon_template (
    id               UUID PRIMARY KEY,
    code             VARCHAR(64) NOT NULL UNIQUE,
    title            VARCHAR(128) NOT NULL,
    type             VARCHAR(16) NOT NULL,
    value            BIGINT NOT NULL,
    min_order_amount BIGINT NOT NULL DEFAULT 0,
    total_quantity   INT NOT NULL,
    issued_quantity  INT NOT NULL DEFAULT 0,
    per_member_limit INT NOT NULL DEFAULT 1,
    validity_days    INT NOT NULL,
    starts_at        TIMESTAMPTZ NOT NULL,
    ends_at          TIMESTAMPTZ NOT NULL,
    status           VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT coupon_template_type_check CHECK (type IN ('AMOUNT', 'PERCENT')),
    CONSTRAINT coupon_template_value_positive CHECK (value > 0),
    CONSTRAINT coupon_template_percent_range CHECK (type <> 'PERCENT' OR value BETWEEN 1 AND 99),
    CONSTRAINT coupon_template_min_order_non_negative CHECK (min_order_amount >= 0),
    CONSTRAINT coupon_template_quantity_check CHECK (total_quantity > 0 AND issued_quantity >= 0
                                                     AND issued_quantity <= total_quantity),
    CONSTRAINT coupon_template_limit_positive CHECK (per_member_limit > 0),
    CONSTRAINT coupon_template_validity_positive CHECK (validity_days > 0),
    CONSTRAINT coupon_template_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'ENDED')),
    CONSTRAINT coupon_template_window_check CHECK (ends_at > starts_at)
);

CREATE TABLE member_coupon (
    id          UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES coupon_template(id),
    member_id   UUID NOT NULL REFERENCES member(id),
    code        VARCHAR(32) NOT NULL UNIQUE,
    status      VARCHAR(16) NOT NULL DEFAULT 'UNUSED',
    issued_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    order_id    UUID,
    CONSTRAINT member_coupon_status_check CHECK (status IN ('UNUSED', 'USED', 'EXPIRED')),
    CONSTRAINT member_coupon_used_at_check CHECK ((status = 'USED') = (used_at IS NOT NULL))
);

CREATE INDEX member_coupon_member_idx ON member_coupon (member_id, status, expires_at);
CREATE INDEX member_coupon_template_idx ON member_coupon (template_id, member_id);
