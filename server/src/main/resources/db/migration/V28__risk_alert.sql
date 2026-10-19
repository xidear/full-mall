-- 风险预警（SEC-004）。告警从审计事件派生，本表只存判定结果。
CREATE TABLE risk_alert (
    id           UUID PRIMARY KEY,
    rule_code    VARCHAR(64)  NOT NULL,
    severity     VARCHAR(16)  NOT NULL,
    subject      VARCHAR(128) NOT NULL,
    detail       VARCHAR(1024),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    acknowledged_at TIMESTAMPTZ,
    CONSTRAINT risk_alert_severity_check CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE INDEX risk_alert_created_idx ON risk_alert (created_at DESC);
CREATE INDEX risk_alert_rule_subject_idx ON risk_alert (rule_code, subject, created_at DESC);