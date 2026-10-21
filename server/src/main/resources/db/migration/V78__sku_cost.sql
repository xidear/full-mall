-- SKU 成本：砍价底价校验、分销 PROFIT 分佣、毛利报表都依赖它。
-- 允许为空：未维护成本时相关功能必须**明确拒绝**，不得按 0 处理。
ALTER TABLE sku ADD COLUMN cost_amount BIGINT;
ALTER TABLE sku ADD CONSTRAINT sku_cost_non_negative CHECK (cost_amount IS NULL OR cost_amount >= 0);
