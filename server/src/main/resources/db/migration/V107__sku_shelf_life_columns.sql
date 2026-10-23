-- 效期策略从 JSON 拆为真实列（6 个字段，全部可索引/可约束）
ALTER TABLE sku
    ADD COLUMN IF NOT EXISTS shelf_batch_tracking      BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS shelf_picking_strategy     VARCHAR(24) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS shelf_min_remaining_days   INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS shelf_near_expiry_days     INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS shelf_near_expiry_action   VARCHAR(24) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN IF NOT EXISTS shelf_auto_block_expired   BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE sku SET
    shelf_batch_tracking    = COALESCE((shelf_life_policy_json->>'batchTracking')::boolean, FALSE),
    shelf_picking_strategy  = COALESCE(NULLIF(shelf_life_policy_json->>'pickingStrategy', ''), 'NONE'),
    shelf_min_remaining_days = COALESCE((shelf_life_policy_json->>'minRemainingShelfLifeDays')::int, 0),
    shelf_near_expiry_days  = COALESCE((shelf_life_policy_json->>'nearExpiryThresholdDays')::int, 0),
    shelf_near_expiry_action = COALESCE(NULLIF(shelf_life_policy_json->>'nearExpiryAction', ''), 'NORMAL'),
    shelf_auto_block_expired = COALESCE((shelf_life_policy_json->>'autoBlockExpired')::boolean, TRUE)
 WHERE shelf_life_policy_json IS NOT NULL;

ALTER TABLE sku
    ADD CONSTRAINT sku_shelf_days_check
        CHECK (shelf_min_remaining_days >= 0 AND shelf_near_expiry_days >= 0);

-- 写入仍走 JSON（兼容既有代码路径），触发器保证列同步
CREATE OR REPLACE FUNCTION sku_shelf_life_sync() RETURNS trigger AS $$
BEGIN
    IF NEW.shelf_life_policy_json IS NOT NULL THEN
        NEW.shelf_batch_tracking     := COALESCE((NEW.shelf_life_policy_json->>'batchTracking')::boolean, FALSE);
        NEW.shelf_picking_strategy   := COALESCE(NULLIF(NEW.shelf_life_policy_json->>'pickingStrategy', ''), 'NONE');
        NEW.shelf_min_remaining_days := COALESCE((NEW.shelf_life_policy_json->>'minRemainingShelfLifeDays')::int, 0);
        NEW.shelf_near_expiry_days   := COALESCE((NEW.shelf_life_policy_json->>'nearExpiryThresholdDays')::int, 0);
        NEW.shelf_near_expiry_action := COALESCE(NULLIF(NEW.shelf_life_policy_json->>'nearExpiryAction', ''), 'NORMAL');
        NEW.shelf_auto_block_expired := COALESCE((NEW.shelf_life_policy_json->>'autoBlockExpired')::boolean, TRUE);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS sku_shelf_life_sync_trg ON sku;
CREATE TRIGGER sku_shelf_life_sync_trg
    BEFORE INSERT OR UPDATE OF shelf_life_policy_json ON sku
    FOR EACH ROW EXECUTE FUNCTION sku_shelf_life_sync();

CREATE INDEX IF NOT EXISTS sku_shelf_expiry_idx
    ON sku (shelf_batch_tracking, shelf_near_expiry_days);
