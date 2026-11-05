-- V11：告警确认与升级（AGENTS §10.5 / §14.4）
--
-- 站内信是告警的默认通道：它不外发、不需要任何配置，落到通知中心就一定能看到。
-- 但「看到了没人处理」和「没人看到」必须能区分，所以升级要有确认位与升级级别：
-- acknowledged_at/by 记录谁在什么时候认领，escalation_level 记录已经升级过几次。
ALTER TABLE risk_alert
    ADD COLUMN IF NOT EXISTS acknowledged_by varchar(64),
    ADD COLUMN IF NOT EXISTS escalation_level int NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS escalated_at timestamptz;

-- 升级巡检（每分钟扫一次）只关心「未恢复且未确认」的行，用部分索引避免全表扫。
CREATE INDEX IF NOT EXISTS risk_alert_escalation_idx
    ON risk_alert (severity, escalation_level)
    WHERE recovered_at IS NULL AND acknowledged_at IS NULL;
