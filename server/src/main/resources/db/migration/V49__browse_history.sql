-- 浏览足迹：一个会员对同一 SKU 只保留一行，重复浏览更新时间
CREATE TABLE member_browse_history (
    member_id UUID NOT NULL REFERENCES member(id),
    sku_id    UUID NOT NULL REFERENCES sku(id),
    viewed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    view_count INT NOT NULL DEFAULT 1,
    PRIMARY KEY (member_id, sku_id)
);

CREATE INDEX member_browse_history_recent_idx ON member_browse_history (member_id, viewed_at DESC);
