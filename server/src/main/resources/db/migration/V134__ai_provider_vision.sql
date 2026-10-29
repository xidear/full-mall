-- AI 供应商增加"视觉能力"标记：
-- 视觉模型可能在远端（公共模型服务商）或本地 CPU 推理服务，主程序只按配置调用，
-- 没配置视觉供应商时"AI 识别图片"接口明确返回 409，不影响其它功能。
ALTER TABLE ai_provider ADD COLUMN vision BOOLEAN NOT NULL DEFAULT FALSE;
