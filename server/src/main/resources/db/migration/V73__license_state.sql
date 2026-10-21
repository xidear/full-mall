-- 许可运行状态：记录最近一次运行时间，用于检测系统时间回拨（改时间续命）
CREATE TABLE license_state (
    id           INT PRIMARY KEY,
    last_seen_at TIMESTAMPTZ NOT NULL,
    license_id   VARCHAR(64),
    customer     VARCHAR(200),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
