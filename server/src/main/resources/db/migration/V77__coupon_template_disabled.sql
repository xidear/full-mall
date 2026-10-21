-- 券模板支持"停用"：停止发放但保留已发实例（不删除模板，便于追溯）
ALTER TABLE coupon_template DROP CONSTRAINT IF EXISTS coupon_template_status_check;
ALTER TABLE coupon_template ADD CONSTRAINT coupon_template_status_check
    CHECK (status IN ('DRAFT', 'ACTIVE', 'ENDED', 'DISABLED'));
