-- 素材的语言变体：同一个"逻辑素材"在不同语言下用不同的图片文件
-- （中文主图带中文文案、英文主图带英文文案）。渲染时按 locale 替换，而不是复制多份商品素材。
CREATE TABLE IF NOT EXISTS asset_locale_variant (
    asset_id   UUID NOT NULL REFERENCES asset (id) ON DELETE CASCADE,
    locale     TEXT NOT NULL,
    url        TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (asset_id, locale)
);

COMMENT ON TABLE asset_locale_variant IS '素材语言变体：asset_id+locale → 本地化图片 URL；商品详情渲染时按语言替换 img 的 src';
