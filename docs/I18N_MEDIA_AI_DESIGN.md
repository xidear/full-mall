# 多语言、媒体本地化与 AI 生成设计

> 日期：2026-10-19
> 触发：用户提出「admin/供应商永远中文，仓库门店默认中文但预留 i18n」「商品主图/视频/详情/描述/标题能否按用户语言自动生成」「翻译用专业 API + 人工审核」「图片视频用 AI 生成并纳入素材库管理」「API 用量能否记录并对账」
> 关联需求：I18N-001~006、AST-001~006、CR-040/CR-041、SEC-004

## 0. 现状（诚实）

| 项 | 现状 | 差距 |
|---|---|---|
| 文本多语言 | `product_translation(product_id, locale, title, description, slug, seo_*)` 已存在 | 无翻译流程、无审核状态、无翻译记忆/术语表；其它实体（类目/属性/法律/促销）仍单语 |
| 媒体本地化 | `sku.image_url` **单一 URL** | 无媒体表、无 locale、无视频、无详情图集合 |
| 素材库 | `asset(name,url,kind,owner)` 最简 | 无文件夹/标签/版本/来源/授权/引用统计/存储 SPI |
| AI 生成 | 无 | 无 provider 配置、无任务、无审核、无成本记录 |
| API 用量 | 无 | 无计量、无预算、无对账 |

## 1. 语言范围策略（先定边界，避免全站被迫多语言）

- **Admin / 供应商门户：固定 `zh-CN`**。文案走消息包但只发中文；不做语言切换器；
  数据层仍保留 `locale` 字段，未来切换零迁移成本。
- **仓库/门店/客服：默认中文**，预留 locale 列与消息包 key，不在 P0/P1 引入多语言。
- **顾客前台：多语言**，`/api/v1` 按 `Accept-Language` + 市场配置解析 locale，回退链
  `目标语言 → 市场默认 → zh-CN`。
- 硬规则：**回退链只用于"没翻译"，不能用于"翻译错了"**；法律/支付文案未审校不得上线（I18N-004）。

## 2. 文本翻译流水线（专业 API + 人工审核）

```
原文(zh-CN) → 机器翻译(Provider) → 草稿 → 人工审校 → 发布
状态：DRAFT | MACHINE | REVIEWED | PUBLISHED | OUTDATED
```

- **Provider 可后台配置**：provider（DeepL / Google Cloud Translation / Azure Translator / LLM）、
  endpoint、API key（加密存储 + 掩码显示）、目标语言列表、术语表。
- **翻译记忆（TM）**：按 `源文哈希 + 语言 + 术语表版本` 命中则复用，**不重复计费**；
  术语表（glossary）优先于机翻（品牌词、品类词、禁译词）。
- **变更检测**：源文修改 → 已有译文标记 `OUTDATED`，前台继续用旧版但后台告警；
  避免"改了个错字，全站英文突然变中文"。
- **审校强制**：`legal`/`payment`/`privacy` 类文案必须 `REVIEWED` 才可 `PUBLISHED`；
  其余允许 `MACHINE` 上线但后台标注"未审校"，且可一键回滚。
- 成熟组件参照：Tolgee 或 Weblate 承接审校工作流（已有选型记录），不自己写翻译后台。

## 3. 媒体本地化（图片/视频必须按语言分套）

新增：

```
asset                      // 统一素材（已有，需扩字段）
  id, kind(IMAGE|VIDEO|DOC), storage_key, mime, width/height, duration_ms,
  checksum, source(UPLOAD|AI_GENERATED|EXTERNAL), license, consent_ref,
  owner_scope(PUBLIC|PRIVATE), status(ACTIVE|ARCHIVED), version, created_by
asset_variant              // 派生/本地化版本
  id, asset_id, locale, role(MAIN|GALLERY|DETAIL|VIDEO), transform, storage_key,
  status(DRAFT|REVIEWED|PUBLISHED), generated_by, prompt_ref, created_at
product_media              // 商品/ SKU 与素材的绑定
  id, owner_type(PRODUCT|SKU), owner_id, locale, role, asset_variant_id, sort_order
```

规则：

1. **前台按 locale 取媒体，回退链固定三级**（2026-10-19 按用户要求修订）：
   `目标语言版本 → 默认版本（英文，且必须是无文字视觉图） → 占位图`。
   - 允许回退到**英文默认图**，因为它对多数市场可读且不含中文；
   - **仍然禁止回退中文图**（除 `zh-CN` 与明确标注中文市场的场景）；
   - 响应带 `mediaSource=SELF|DEFAULT|PLACEHOLDER`，前端可据此显示"示意图/本地化中"角标，
     不允许把默认图伪装成本地化图（避免误导）。
   - 默认图同样是素材库中的一条 `asset_variant`（`locale='default'`），可后台替换，
     不在代码里写死 URL。
2. 中文原图允许作为 `zh-CN` 版本；其它语言版本必须独立审核，因为"图里有中文"对西语用户等于无效信息。
3. 媒体与文案同生命周期：源商品更新 → 相关媒体标 `OUTDATED`，需重新生成或确认。

## 4. AI 生成（图片/视频）

- **不要指望模型把外文文字画对**。成熟做法：生成**无文字**的视觉主图，再由前端/渲染服务
  叠加本地化文案（HTML 模板 + 字体 + 安全边距，导出静态图用于第三方渠道）。
- 生成输入：原始商品实拍（image-to-image） + 品牌规范（色板/构图/禁用元素） + locale 风格提示；
  保留原图作为 reference，不让模型"发明"不存在的商品特征（虚假宣传风险）。
- 视频：首帧/关键帧由图片生成，再交视频模型做轻运动（推拉/旋转/场景切换）；
  时长与分辨率按投放渠道模板约束；成本按秒计费，必须走异步任务与预算闸门。
- **人工审核强制**：AI 生成的商品媒体默认 `DRAFT`，审核通过才 `PUBLISHED`；
  涉及功效/认证/价格表述的图片必须人工复核（合规）。
- Provider 抽象：`MediaGenerationProvider`（submit/status/result/cancel），
  Mock 实现用于演示；生产缺 key 时明确报错，不静默降级（与支付一致）。

## 5. API 用量记录与对账

```
ai_usage(id, provider, model, operation(TRANSLATE|IMAGE|VIDEO|OCR),
         request_id, idempotency_key, related_type, related_id,
         input_units(tokens/chars), output_units(images/seconds),
         unit_price_snapshot, cost_amount, currency,
         status(SUCCEEDED|FAILED|REFUNDED), created_at, created_by)
```

- **单价快照**：写入时冻结当时的价目，供应商调价不改变历史成本。
- **幂等**：`idempotency_key` 唯一约束，重试不重复计费。
- **预算闸门**：按日/月/供应商/操作类型设上限，超限拒绝并告警（SEC-004 风险预警复用）。
- **对账**：月初导入供应商账单（CSV），与 `ai_usage` 按 `provider + request_id` 对账；
  差异分类：我方多记/少记/供应商赠送/失败退款。差异超阈值告警。
- 报表：按语言、按商品、按操作类型看单位成本（例如"一条西语详情图成本 X 分"）。

## 6. 合规与风险

| 风险 | 处理 |
|---|---|
| 商品图上传第三方 AI（数据出境） | 供应商白名单 + 数据出境评估；敏感类目（人像/证件）禁止外发 |
| AI 生成图与实物不符 | 保留原图 reference；生成结果人工审核；对外标注"示意图"（如适用） |
| 机翻误译法律/支付文案 | 强制人工审校 + 发布门禁 + 一键回滚 |
| 成本失控 | 预算闸门 + 幂等 + 缓存/TM + 批量任务 |
| 版权/肖像权 | `license` 与 `consent_ref` 字段；来源不明的素材禁止入库 |

## 7. 实施顺序（并入 v3 计划 G 组）

1. **G6 文案翻译流水线**：provider 配置 + TM + 状态机 + 审校门禁（先做商品标题/描述）。
2. **G7 媒体本地化模型**：`asset` 扩字段 + `asset_variant` + `product_media`，前台按 locale 取图。
3. **G8 AI 生成**：`MediaGenerationProvider` + 异步任务 + 审核队列 + Mock 实现。
4. **G9 用量与对账**：`ai_usage` + 预算闸门 + 月度对账。
5. **G10 无文字主图 + 本地化叠字渲染**：模板渲染服务，输出静态图供渠道使用。

## 8. 需要业务确认

1. 目标语言优先级（es / en / ar / fr / ru？）与首发市场；
2. 翻译是否允许"机翻直接上线"（当前设计：除法律/支付外允许，但标注未审校）；
3. AI 生成是否允许使用第三方云（涉及商品图出境）；
4. 视频是否必做，还是先图片 + 详情图；
5. 单条商品的本地化预算上限（决定是否自动批量生成）。