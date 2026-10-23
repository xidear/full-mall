-- 阶梯促销（C9）：满赠 / 第二件半价。默认不启用，由 app.promotion.ladder-enabled 打开。
CREATE TABLE IF NOT EXISTS promotion_ladder (
    id            UUID PRIMARY KEY,
    code          VARCHAR(64) NOT NULL UNIQUE,
    kind          VARCHAR(16) NOT NULL,
    threshold_fen BIGINT NOT NULL DEFAULT 0,
    gift_sku_id   UUID REFERENCES sku(id),
    gift_quantity INT NOT NULL DEFAULT 0,
    priority      INT NOT NULL DEFAULT 0,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    ends_at       TIMESTAMPTZ,
    CONSTRAINT promotion_ladder_kind_check CHECK (kind IN ('GIFT', 'SECOND_HALF')),
    CONSTRAINT promotion_ladder_gift_check
        CHECK (kind <> 'GIFT' OR (gift_sku_id IS NOT NULL AND gift_quantity > 0))
);

CREATE INDEX IF NOT EXISTS promotion_ladder_active_idx ON promotion_ladder (active, kind, priority);
