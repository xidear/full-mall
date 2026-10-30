package com.fullmall.asset.app;

import com.fullmall.ai.media.AiMediaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AI 生成的素材入库后：用提示词补"描述 + 标签"，并建立向量索引。
 *
 * <p>为什么要这么做：素材文件名是 "AI mock-image &lt;uuid&gt;"，只按标题搜索永远找不到；
 * 用提示词当描述，运营搜"白底马克杯""厨房场景"就能命中刚生成的图。
 *
 * <p>为什么用事件：AiMediaService 与 AssetSearchService 之间经 SemanticIndexService 形成依赖环，
 * 事件把两者解耦。
 */
@Component
public class AiAssetIndexingListener {

    private static final Logger log = LoggerFactory.getLogger(AiAssetIndexingListener.class);

    private final AssetSearchService assets;
    private final AssetImageIndexService imageIndex;

    public AiAssetIndexingListener(AssetSearchService assets, AssetImageIndexService imageIndex) {
        this.assets = assets;
        this.imageIndex = imageIndex;
    }

    /** 生成成功 → 写描述/标签并重建索引；失败必须抛出（生成成功但检索退化，不能静默）。 */
    @EventListener
    public void onGenerated(AiMediaService.AssetGenerated event) {
        UUID assetId = event.assetId();
        String prompt = event.prompt() == null ? "" : event.prompt();
        try {
            assets.updateMeta(assetId, keywords(prompt), prompt.isBlank() ? null : prompt);
            // 生成的图片同样要有"图像内容向量"，否则用文字搜图搜不到刚生成的素材
            if ("IMAGE".equalsIgnoreCase(event.kind()) && imageIndex.available()) {
                imageIndex.index(assetId, null);
            }
        } catch (RuntimeException error) {
            log.error("AI 素材打标签/建索引失败 asset={} prompt={}", assetId, prompt, error);
            throw error;
        }
    }

    /** 从提示词抽取标签（规则见 {@link AssetTextKeywords}）。 */
    static List<String> keywords(String prompt) {
        return AssetTextKeywords.of(prompt);
    }
}
