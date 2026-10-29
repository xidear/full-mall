/**
 * AI 媒体能力（文生图、图像编辑、embedding 与知识检索）。
 *
 * <p>为什么显式暴露：asset 模块的素材搜索与索引需要调用媒体能力（{@code AiMediaService}），
 * 而 Modulith 默认只允许访问其它模块的 API/NamedInterface。此前没有声明，
 * 导致 `ModulithStructureTest` 长期失败（既有缺陷，2026-10-30 审计时修掉）。
 *
 * <p>暴露范围是**服务入口与它的返回值**，不是整个内部实现：adapter/clip 等实现细节仍在
 * 未命名的子包里，跨模块不能直接引用。
 */
@org.springframework.modulith.NamedInterface("ai-media")
package com.fullmall.ai.media;
