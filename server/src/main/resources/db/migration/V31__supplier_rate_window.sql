-- 供应商限流固定窗口（SUP-033）。放数据库而非内存：多副本共享同一计数。
CREATE TABLE supplier_rate_window (
    access_key    VARCHAR(64)  NOT NULL,
    window_start  TIMESTAMPTZ  NOT NULL,
    request_count INT          NOT NULL,
    PRIMARY KEY (access_key, window_start),
    CONSTRAINT supplier_rate_window_count_positive CHECK (request_count > 0)
);

CREATE INDEX supplier_rate_window_start_idx ON supplier_rate_window (window_start);