-- 多供应商能力（短信/邮件/公众号/告警群）：同一能力允许同时启用多家，
-- 由 integration_route 决定「哪个动作用哪家」。
--
-- V1 的 integration_config_one_enabled 是「一个能力只能有一个启用供应商」的结构约束，
-- 直接挡住了「验证码走阿里、告警走腾讯」这类需求。这里去掉它。
-- 单供应商能力（支付/存储/物流/AI…）仍由 IntegrationConfigService 在写入时保证唯一，
-- 不在 SQL 里复制一份能力清单（避免双源真相：加一个能力要改两处）。

DROP INDEX IF EXISTS public.integration_config_one_enabled;
