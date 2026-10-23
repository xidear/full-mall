-- 定时任务跨实例租约：多实例部署时同一任务只会被一个实例执行。
CREATE TABLE IF NOT EXISTS scheduled_task_lease (
    code        VARCHAR(64) PRIMARY KEY,
    holder      VARCHAR(128) NOT NULL,
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL
);
