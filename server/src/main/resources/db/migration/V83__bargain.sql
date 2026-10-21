-- 砍价：名义砍价、实质拉新。底价由运营填写（可低于成本，需显式确认并留原因）。
CREATE TABLE bargain_activity (
    id             UUID PRIMARY KEY,
    sku_id         UUID NOT NULL REFERENCES sku(id),
    floor_price    BIGINT NOT NULL,
    currency       CHAR(3) NOT NULL,
    max_cut        BIGINT NOT NULL,
    min_helpers    INT NOT NULL,
    cut_min        BIGINT NOT NULL,
    cut_max        BIGINT NOT NULL,
    expires_hours  INT NOT NULL,
    allow_below_cost BOOLEAN NOT NULL DEFAULT FALSE,
    below_cost_reason VARCHAR(255),
    status         VARCHAR(16) NOT NULL,
    starts_at      TIMESTAMPTZ NOT NULL,
    ends_at        TIMESTAMPTZ NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT bargain_activity_floor_check CHECK (floor_price > 0),
    CONSTRAINT bargain_activity_helpers_check CHECK (min_helpers BETWEEN 3 AND 30),
    CONSTRAINT bargain_activity_cut_check CHECK (cut_min > 0 AND cut_max >= cut_min),
    CONSTRAINT bargain_activity_expiry_check CHECK (expires_hours BETWEEN 12 AND 72),
    CONSTRAINT bargain_activity_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'ENDED'))
);

CREATE TABLE bargain_session (
    id            UUID PRIMARY KEY,
    activity_id   UUID NOT NULL REFERENCES bargain_activity(id),
    member_id     UUID NOT NULL REFERENCES member(id),
    current_price BIGINT NOT NULL,
    helper_count  INT NOT NULL DEFAULT 0,
    status        VARCHAR(16) NOT NULL,
    expires_at    TIMESTAMPTZ NOT NULL,
    order_id      UUID,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT bargain_session_status_check CHECK (status IN ('ONGOING', 'READY', 'EXPIRED', 'ORDERED')),
    CONSTRAINT bargain_session_price_check CHECK (current_price > 0),
    -- 同一会员同一活动只能发起一次
    CONSTRAINT bargain_session_unique UNIQUE (activity_id, member_id)
);

CREATE TABLE bargain_help (
    id             UUID PRIMARY KEY,
    session_id     UUID NOT NULL REFERENCES bargain_session(id),
    helper_member_id UUID NOT NULL REFERENCES member(id),
    cut_amount     BIGINT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT bargain_help_cut_check CHECK (cut_amount > 0),
    -- 助力人去重：同一场次每人只能帮一次
    CONSTRAINT bargain_help_unique UNIQUE (session_id, helper_member_id)
);

CREATE INDEX bargain_activity_sku_idx ON bargain_activity (sku_id, status);
CREATE INDEX bargain_session_member_idx ON bargain_session (member_id, status);
