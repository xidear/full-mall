-- 逐笔积分余额：支持按笔 FIFO 过期，避免"总额过期"算不清
ALTER TABLE points_ledger ADD COLUMN remaining_points BIGINT NOT NULL DEFAULT 0;

-- 存量回填：EARN 视为未消耗（保守估计：宁可多留，不凭空作废顾客积分）
UPDATE points_ledger SET remaining_points = points WHERE entry_type = 'EARN';

CREATE INDEX points_ledger_fifo_idx
    ON points_ledger (member_id, created_at)
    WHERE entry_type = 'EARN' AND remaining_points > 0;
