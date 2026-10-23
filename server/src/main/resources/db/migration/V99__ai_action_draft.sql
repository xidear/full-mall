-- AI 受控写操作（N3）：草稿 → 双人审批 → 执行
CREATE TABLE ai_action_draft (
    id             UUID PRIMARY KEY,
    action_type    VARCHAR(48) NOT NULL,
    payload        JSONB NOT NULL,
    status         VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    requested_by   VARCHAR(64) NOT NULL,
    approved_by_1  VARCHAR(64),
    approved_by_2  VARCHAR(64),
    rejected_by    VARCHAR(64),
    reject_reason  VARCHAR(255),
    executed_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ai_action_draft_status_check
        CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'EXECUTED')),
    CONSTRAINT ai_action_draft_two_approvers_check
        CHECK (approved_by_2 IS NULL OR approved_by_1 IS DISTINCT FROM approved_by_2)
);

CREATE INDEX ai_action_draft_status_idx ON ai_action_draft (status, created_at DESC);
