-- 导出任务审计（SEC-002）。追加写，不提供更新/删除入口（SEC-005）。
CREATE TABLE export_task (
    id          UUID PRIMARY KEY,
    resource    VARCHAR(64)  NOT NULL,
    scope_kind  VARCHAR(16)  NOT NULL,
    actor       VARCHAR(128) NOT NULL,
    row_count   INT          NOT NULL,
    columns     VARCHAR(512) NOT NULL,
    request_id  VARCHAR(64),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT export_task_row_count_non_negative CHECK (row_count >= 0)
);

CREATE INDEX export_task_created_idx ON export_task (created_at DESC);
CREATE INDEX export_task_actor_idx ON export_task (actor, created_at DESC);