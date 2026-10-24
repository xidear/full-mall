-- 新增 embedding 协议（本机 Ollama / vLLM 均可），用于知识库检索与相似素材推荐
ALTER TABLE ai_provider DROP CONSTRAINT IF EXISTS ai_provider_protocol_check;
ALTER TABLE ai_provider
    ADD CONSTRAINT ai_provider_protocol_check
    CHECK (protocol IN ('openai-images', 'dashscope-image', 'dashscope-video',
                        'openai-chat', 'openai-embedding'));
