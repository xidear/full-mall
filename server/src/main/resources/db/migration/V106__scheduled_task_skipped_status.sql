-- 多实例租约引入 SKIPPED（另一实例在跑），约束需允许该状态
ALTER TABLE scheduled_task DROP CONSTRAINT IF EXISTS scheduled_task_last_status_check;
ALTER TABLE scheduled_task
    ADD CONSTRAINT scheduled_task_last_status_check
    CHECK (last_status IS NULL OR last_status IN ('SUCCESS', 'FAILED', 'SKIPPED'));
