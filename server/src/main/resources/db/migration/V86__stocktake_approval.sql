-- 盘点差异审批：大差异不直接调账，需 ADMIN 审批
ALTER TABLE stocktake ADD COLUMN approval_status VARCHAR(16) NOT NULL DEFAULT 'NONE';
ALTER TABLE stocktake ADD COLUMN approved_at TIMESTAMPTZ;
ALTER TABLE stocktake ADD COLUMN approved_by VARCHAR(64);
ALTER TABLE stocktake ADD CONSTRAINT stocktake_approval_check
    CHECK (approval_status IN ('NONE', 'PENDING', 'APPROVED', 'REJECTED'));
