-- 三方通道「用途 → 供应商」绑定（ADR-0018）。
-- 同一能力可以并存多个供应商（阿里云 + 腾讯云）；本表决定某个业务动作实际走谁。
-- 未绑定 = 该用途没有通道，调用点必须明确失败，禁止回退到「第一个启用的供应商」。
-- recipient 是该用途的收件人（告警短信的手机号 / 告警邮箱 / 公众号 openid）；
-- 群机器人不需要收件人，订单通知按业务数据里的手机号投递。
CREATE TABLE integration_route (
    capability varchar(32) NOT NULL,
    usage_key  varchar(64) NOT NULL,
    provider   varchar(32) NOT NULL,
    recipient  varchar(128),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT integration_route_pk PRIMARY KEY (capability, usage_key)
);

-- 告警必须能回答「到底有没有发出去」；同时把 FATAL 纳入分级（AGENTS §10.5）。
ALTER TABLE risk_alert
    ADD COLUMN alerted boolean NOT NULL DEFAULT false,
    ADD COLUMN alerted_at timestamptz,
    ADD COLUMN alert_channel varchar(64),
    ADD COLUMN alert_error varchar(512),
    ADD COLUMN recovered_at timestamptz;

ALTER TABLE risk_alert DROP CONSTRAINT risk_alert_severity_check;
ALTER TABLE risk_alert
    ADD CONSTRAINT risk_alert_severity_check
    CHECK (severity IN ('FATAL', 'HIGH', 'MEDIUM', 'LOW'));

-- 回填：升级前只有一个启用供应商的能力，把它的用途全绑过去，
-- 否则升级后所有通道会「突然消失」。多供应商并存时故意不猜，留给运营在后台勾选。
INSERT INTO integration_route (capability, usage_key, provider)
SELECT single.capability, u.usage_key, single.provider
  FROM (SELECT capability, MIN(provider) AS provider
          FROM integration_config WHERE enabled
         GROUP BY capability HAVING COUNT(*) = 1) single
  JOIN (VALUES ('sms', 'OTP_LOGIN'), ('sms', 'ORDER_EVENT'), ('sms', 'ALERT_FATAL'),
               ('email', 'OTP_LOGIN'), ('email', 'ORDER_EVENT'), ('email', 'ALERT_FATAL'),
               ('wechat', 'ORDER_EVENT'),
               ('alert_im', 'ALERT_FATAL'), ('alert_im', 'ALERT_RECOVERY'))
       AS u(capability, usage_key) ON u.capability = single.capability;
