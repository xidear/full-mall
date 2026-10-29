package com.fullmall.catalog.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 后台商品详情聚合视图（设计审查 2026-10-28）。
 *
 * <p>为什么聚合：编辑页需要商品、SKU、素材、各语言译文——过去分散成 4 个请求，
 * 任何一个失败都只弹一个 toast，页面留着半截旧数据（切语言看到上一个语言的内容就是这样来的）。
 * 一次请求返回"这一屏需要的全部事实"，前端只有"加载中/成功/失败"三种状态。
 */
public record ProductDetailView(
        UUID id,
        String title,
        String description,
        UUID categoryId,
        String status,
        String inventoryTrackingLevel,
        boolean shelfLifeManaged,
        List<com.fullmall.catalog.domain.Sku> skus,
        List<Map<String, Object>> media,
        List<TranslationEntry> translations,
        UUID skuMatrixTemplateId,
        String defaultLocale,
        List<String> enabledLocales,
        /** 富文本详情（后台编辑用；已过服务端白名单清洗） */
        String detailHtml) {

    /**
     * 单个语言的译文。
     *
     * @param present 是否已有译文；false 时其余字段为 null（**不再用 404 表达"还没有译文"**）
     */
    public record TranslationEntry(String locale, boolean present, String title, String description,
                                  String imageUrl, String status) {
    }
}
