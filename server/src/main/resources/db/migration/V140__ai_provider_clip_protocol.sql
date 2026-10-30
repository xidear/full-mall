-- ai_provider.protocol 是 CHECK 枚举，新增多模态向量协议必须同步加进去，
-- 否则配置 CLIP 供应商时直接 500（DataIntegrityViolationException）。
ALTER TABLE ai_provider DROP CONSTRAINT IF EXISTS ai_provider_protocol_check;
ALTER TABLE ai_provider ADD CONSTRAINT ai_provider_protocol_check
    CHECK (protocol IN (
        'openai-images', 'dashscope-image', 'dashscope-video', 'dashscope-multimodal',
        'ark-image', 'ark-video', 'minimax-video', 'openai-chat', 'openai-embedding',
        'clip-http'
    ));
