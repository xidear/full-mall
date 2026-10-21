-- 导出处理默认开启：默认关闭会让导出任务永远停在 PENDING（用户以为系统坏了）。
-- 仅对"从未被显式开启过"的存量行做一次性修正；后续仍可由后台自由开关。
UPDATE scheduled_task SET enabled = TRUE, updated_at = now()
 WHERE code = 'export.worker' AND enabled = FALSE;
