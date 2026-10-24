package com.fullmall.ai.media.adapter;

import com.fullmall.ai.media.ProviderConfig;

import java.util.Map;

/** 生成适配器端口：每个协议一个实现，供应商数据来自数据库。 */
public interface MediaGenerationAdapter {

    /** 支持的协议标识（与 `ai_provider.protocol` 对应）。 */
    String protocol();

    GeneratedMedia generate(ProviderConfig provider, String prompt, String negative,
                            Map<String, Object> params);
}
