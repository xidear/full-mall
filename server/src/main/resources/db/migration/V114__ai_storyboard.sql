-- 视频分镜：一个分镜本 = 若干镜头（提示词 + 引导图 + 时长），逐镜头生成后汇总
CREATE TABLE ai_storyboard (
    id         UUID PRIMARY KEY,
    title      VARCHAR(160) NOT NULL,
    product_id UUID REFERENCES product(id),
    sku_id     UUID REFERENCES sku(id),
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai_storyboard_shot (
    id              UUID PRIMARY KEY,
    storyboard_id   UUID NOT NULL REFERENCES ai_storyboard(id) ON DELETE CASCADE,
    sort_order      INT NOT NULL,
    prompt          TEXT NOT NULL,
    guide_asset_id  UUID,
    duration_sec    INT NOT NULL DEFAULT 5,
    job_id          UUID,
    asset_id        UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ai_storyboard_shot_unique UNIQUE (storyboard_id, sort_order)
);

CREATE INDEX ai_storyboard_shot_idx ON ai_storyboard_shot (storyboard_id, sort_order);
COMMENT ON COLUMN ai_storyboard_shot.guide_asset_id IS '分镜引导图（首帧）素材 id，来自素材库';
