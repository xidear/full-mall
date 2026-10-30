/**
 * CLIP 多模态向量客户端（文字与图片映射到同一向量空间）。
 *
 * <p>为什么显式暴露：asset 模块的图片索引需要调用 {@code ClipEmbeddingClient} 生成图像向量，
 * 而 Modulith 只允许跨模块访问 API/NamedInterface。此前未声明，导致
 * `ModulithStructureTest` 报 `Module 'asset' depends on non-exposed type ...ai.clip...`。
 *
 * <p>它本身是**可选能力**：服务未启动时调用会失败，但素材库其它功能不受影响
 * （见 tools/clip-service/README.md 与依赖矩阵）。
 */
@org.springframework.modulith.NamedInterface("ai-clip")
package com.fullmall.ai.clip;
