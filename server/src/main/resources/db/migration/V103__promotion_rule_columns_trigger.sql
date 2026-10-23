-- 让数据库自己维护 promotion 的投影列，避免"程序忘写列"这类问题。
-- rules 仍是唯一事实来源；四列是查询投影，由触发器同步。
CREATE OR REPLACE FUNCTION promotion_sync_rule_columns() RETURNS trigger AS $$
BEGIN
    IF NEW.rules IS NOT NULL THEN
        NEW.threshold_amount := NULLIF(NEW.rules->>'thresholdAmount', '')::bigint;
        NEW.rule_value       := NULLIF(NEW.rules->>'value', '')::bigint;
        NEW.channel          := COALESCE(NULLIF(NEW.rules->>'channel', ''), 'ONLINE');
        NEW.exclusive_group  := NULLIF(NEW.rules->>'exclusiveGroup', '');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS promotion_sync_rule_columns_trg ON promotion;
CREATE TRIGGER promotion_sync_rule_columns_trg
    BEFORE INSERT OR UPDATE OF rules ON promotion
    FOR EACH ROW EXECUTE FUNCTION promotion_sync_rule_columns();
