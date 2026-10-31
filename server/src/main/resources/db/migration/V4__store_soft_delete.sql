-- 门店软删除：与 active（营业/可选）分离。
-- active=false 仍可在后台看到并恢复营业；deleted_at 非空表示“已归档”，
-- 默认列表/自提/员工绑定都不再出现，只能通过「已删除」筛选看到并恢复。
ALTER TABLE store ADD COLUMN IF NOT EXISTS deleted_at timestamp with time zone;

-- 常用查询都是 deleted_at IS NULL，部分索引避免全表扫描
CREATE INDEX IF NOT EXISTS store_active_not_deleted_idx
    ON store (active, name)
    WHERE deleted_at IS NULL;