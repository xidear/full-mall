-- 图形化定时任务：cron 存库，后台可改、可启停、可手动触发。
CREATE TABLE scheduled_task (
    code          VARCHAR(64) PRIMARY KEY,
    name          VARCHAR(128) NOT NULL,
    cron          VARCHAR(64) NOT NULL,
    enabled       BOOLEAN NOT NULL DEFAULT FALSE,
    last_run_at   TIMESTAMPTZ,
    last_status   VARCHAR(16),
    last_error    VARCHAR(500),
    last_duration_ms BIGINT,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT scheduled_task_status_check
        CHECK (last_status IS NULL OR last_status IN ('SUCCESS', 'FAILED'))
);
