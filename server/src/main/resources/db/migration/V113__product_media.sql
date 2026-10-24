-- 商品/规格素材关系：多图 + 主图/详情图/视频/规格专用图，统一指向素材库 asset
CREATE TABLE product_media (
    id         UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    sku_id     UUID REFERENCES sku(id) ON DELETE CASCADE,
    asset_id   UUID NOT NULL,
    role       VARCHAR(16) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    source     VARCHAR(16) NOT NULL DEFAULT 'UPLOAD',
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT product_media_role_check
        CHECK (role IN ('MAIN', 'GALLERY', 'DETAIL', 'VIDEO', 'SPEC')),
    CONSTRAINT product_media_source_check CHECK (source IN ('UPLOAD', 'AI')),
    CONSTRAINT product_media_unique UNIQUE (product_id, sku_id, asset_id, role)
);

CREATE INDEX product_media_product_idx ON product_media (product_id, role, sort_order);
CREATE INDEX product_media_sku_idx ON product_media (sku_id, role, sort_order);

-- 同一商品只能有一个主图（sku 为空的主图）
CREATE UNIQUE INDEX product_media_single_main_idx
    ON product_media (product_id) WHERE role = 'MAIN' AND sku_id IS NULL;

-- 素材库增加用途维度：只允许商品/规格相关素材入库，审批附件等走其它存储
ALTER TABLE asset ADD COLUMN IF NOT EXISTS purpose VARCHAR(24);
UPDATE asset SET purpose = 'PRODUCT' WHERE purpose IS NULL;
ALTER TABLE asset
    ADD CONSTRAINT asset_purpose_check
        CHECK (purpose IN ('PRODUCT', 'SPEC', 'VIDEO', 'AI_GENERATED', 'OTHER_SYSTEM'));
COMMENT ON COLUMN asset.purpose IS
    'PRODUCT/SPEC/VIDEO/AI_GENERATED 进素材库；OTHER_SYSTEM 给审批附件等非商品素材，不在素材库展示';
