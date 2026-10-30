-- 定时任务状态约束漏了 SKIPPED：
-- 任务被跳过时 worker 写 last_status='SKIPPED'，撞 check 约束 → 每轮都抛
-- DataIntegrityViolationException（日志里每 10 秒一条），跳过原因也存不下来。
ALTER TABLE scheduled_task DROP CONSTRAINT IF EXISTS scheduled_task_status_check;
ALTER TABLE scheduled_task ADD CONSTRAINT scheduled_task_status_check
    CHECK (last_status IS NULL OR last_status IN ('SUCCESS', 'FAILED', 'SKIPPED', 'RUNNING'));
