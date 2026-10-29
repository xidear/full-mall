-- 素材库：标签 + 图片描述（语义检索的内容来源）；商品：富文本详情
ALTER TABLE asset ADD COLUMN tags text[] NOT NULL DEFAULT '{}';
ALTER TABLE asset ADD COLUMN caption text;
ALTER TABLE asset ADD COLUMN indexed_at TIMESTAMPTZ;

CREATE INDEX asset_tags_gin ON asset USING gin (tags);

-- 商品详情是富文本（可含图片/视频/文字），"详情图"只是其中的图片素材
ALTER TABLE product ADD COLUMN detail_html TEXT;
