-- 增加火山方舟（Seedream/Seedance）与千问多模态图像编辑协议
ALTER TABLE ai_provider DROP CONSTRAINT IF EXISTS ai_provider_protocol_check;
ALTER TABLE ai_provider
    ADD CONSTRAINT ai_provider_protocol_check
    CHECK (protocol IN ('openai-images', 'dashscope-image', 'dashscope-video',
                        'dashscope-multimodal', 'ark-image', 'ark-video', 'minimax-video',
                        'openai-chat', 'openai-embedding'));
