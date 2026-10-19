-- 操作日志（SEC-003）。追加写；不提供更新/删除入口（SEC-005）。
CREATE TABLE audit_event (
    id          UUID PRIMARY KEY,
    event_type  VARCHAR(64)  NOT NULL,
    actor       VARCHAR(128) NOT NULL,
    resource    VARCHAR(64)  NOT NULL,
    resource_id VARCHAR(64),
    detail      VARCHAR(1024),
    request_id  VARCHAR(64),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX audit_event_type_created_idx ON audit_event (event_type, created_at DESC);
CREATE INDEX audit_event_actor_created_idx ON audit_event (actor, created_at DESC);