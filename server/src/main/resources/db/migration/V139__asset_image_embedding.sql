-- 图像内容向量（CLIP）：与 knowledge_embedding 分开存，因为维度/模型不同（512 vs 768），
-- 混在一张表里会让"维度不一致"变成隐形需求。
CREATE TABLE IF NOT EXISTS asset_image_embedding (
    asset_id   UUID PRIMARY KEY REFERENCES asset (id) ON DELETE CASCADE,
    vector     float8[] NOT NULL,
    model      TEXT NOT NULL,
    provider   TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE asset_image_embedding IS '素材图像向量（多模态/CLIP），用于"用一句话搜图片内容"；维度由模型决定，与 knowledge_embedding 分开';
