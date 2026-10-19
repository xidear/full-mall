-- 异步导出任务（A6）：大导出不再阻塞请求线程
CREATE TABLE export_job (
    id           UUID PRIMARY KEY,
    requester    VARCHAR(128) NOT NULL,
    resource     VARCHAR(64)  NOT NULL,
    projection   VARCHAR(32)  NOT NULL,     -- OPS / FINANCE 等角色投影
    params       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    row_count    INT,
    file_path    VARCHAR(1024),
    error        VARCHAR(1024),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    started_at   TIMESTAMPTZ,
    finished_at  TIMESTAMPTZ,
    expires_at   TIMESTAMPTZ,
    CONSTRAINT export_job_status_check
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'EXPIRED'))
);

CREATE INDEX export_job_pending_idx ON export_job (status, created_at);
CREATE INDEX export_job_requester_idx ON export_job (requester, created_at DESC);
