-- 变更水位所需的时间列：sku/product 之前只有 created_at。
-- 用触发器维护，保证任何写入路径（后台、脚本、SQL）都会推进 updated_at，
-- 并强制单调递增（同一毫秒内多次修改也能被水位识别）。
ALTER TABLE product ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
ALTER TABLE sku ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE product SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE sku SET updated_at = created_at WHERE updated_at IS NULL;

ALTER TABLE product ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE sku ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE product ALTER COLUMN updated_at SET DEFAULT now();
ALTER TABLE sku ALTER COLUMN updated_at SET DEFAULT now();

CREATE OR REPLACE FUNCTION touch_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at := GREATEST(now(), OLD.updated_at + interval '1 millisecond');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS product_touch_updated_at ON product;
CREATE TRIGGER product_touch_updated_at BEFORE UPDATE ON product
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

DROP TRIGGER IF EXISTS sku_touch_updated_at ON sku;
CREATE TRIGGER sku_touch_updated_at BEFORE UPDATE ON sku
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- 知识同步水位：每个 SKU 最后一次同步时间
CREATE TABLE knowledge_sync_state (
    sku_id     UUID PRIMARY KEY,
    synced_at  TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
