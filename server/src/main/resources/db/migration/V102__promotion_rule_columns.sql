-- promotion.rules 中的高频查询字段提升为真实列（可索引、可约束）
-- 依据：PromotionPreviewService / PromotionAdminService 一直用 rules->>'thresholdAmount' 等做查询与筛选
ALTER TABLE promotion
    ADD COLUMN IF NOT EXISTS threshold_amount BIGINT,
    ADD COLUMN IF NOT EXISTS rule_value      BIGINT,
    ADD COLUMN IF NOT EXISTS channel         VARCHAR(16),
    ADD COLUMN IF NOT EXISTS exclusive_group VARCHAR(64);

-- 回填既有数据（JSON 仍是写入来源，列是查询投影；后续写入两处同时维护）
UPDATE promotion SET
    threshold_amount = NULLIF(rules->>'thresholdAmount', '')::bigint,
    rule_value       = NULLIF(rules->>'value', '')::bigint,
    channel          = COALESCE(NULLIF(rules->>'channel', ''), 'ONLINE'),
    exclusive_group  = NULLIF(rules->>'exclusiveGroup', '')
 WHERE rules IS NOT NULL;

CREATE INDEX IF NOT EXISTS promotion_active_channel_idx
    ON promotion (channel, status, starts_at, ends_at);

COMMENT ON COLUMN promotion.threshold_amount IS '门槛（分）；由 rules.thresholdAmount 投影，写入时同步';
COMMENT ON COLUMN promotion.rule_value IS '满减额或折扣百分比；由 rules.value 投影';
