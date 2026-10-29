# ADR-006：规格矩阵改为「可复用模板」（不含价格）

- 日期：2026-10-28
- 状态：已实施
- 相关：ADR-003（SKU options 关联表）、docs/ADMIN_DESIGN_REVIEW_2026-10-28.md

## 背景

原「SKU 规格矩阵」页要求运营：

1. 手填**商品 ID**（一长串 UUID）；
2. 用纯文本描述维度：`颜色:黑,白` 换行 `存储:256G,512G`；
3. 用纯文本描述禁用组合：`颜色:白,存储:512G`；
4. 填 **SKU 前缀 + 价格**，直接生成 SKU。

问题：

- 纯文本格式易错，出错只能靠后端报错；
- 同一个规格组合（例如手机的颜色×容量）要在每个商品上重抄一遍；
- 价格写在"组合定义"里，但组合本身与商品无关——运营会误以为价格会跟着模板走；
- 矩阵与商品没有关联记录，事后无法回答"这批 SKU 是哪个模板生成的"。

## 决策

**模板只描述「维度 + 取值 + 禁用组合」，不含价格；商品选择模板后，应用时输入价格生成 SKU。**

表结构（`V123__admin_product_detail_i18n_matrix.sql`）：

- `sku_matrix_template(id, code, name, note, created_at, updated_at)`
- `sku_matrix_dimension(id, template_id, name, sort_order)`
- `sku_matrix_dimension_value(id, dimension_id, name, sort_order)`
- `sku_matrix_disabled(template_id, combination_json JSONB)` —— 与 `SkuMatrixMultiService` 的 disabled 规则同构
- `product.sku_matrix_template_id` —— 商品 → 模板的多对一关联

API：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/v1/sku-matrix-templates` | 列表（含组合数、可用组合数、被引用商品数） |
| POST | `/admin/v1/sku-matrix-templates` | 新建 |
| PUT | `/admin/v1/sku-matrix-templates/{id}` | 修改（整体替换维度/禁用组合） |
| DELETE | `/admin/v1/sku-matrix-templates/{id}` | 删除；被商品引用时 409 |
| POST | `/admin/v1/sku-matrix-templates/{id}/apply` | 用模板为商品生成 SKU，**此时才输入价格** |

前端：

- 维度/取值用**标签**编辑（输入后回车或点添加、点 × 删除）；
- 组合用**勾选框**列出（取消勾选 = 禁用组合），不再让运营写字符串；
- 商品编辑页「规格」标签里"选择模板 → 前缀 → 价格 → 应用"。

## 后果

正向：

- 模板可复用，减少重复录入与格式错误；
- 价格语义清晰：价格属于"商品 × SKU"，不属于模板；
- 商品与模板有关联记录，可核对来源；删除模板前必须先解绑，不会静默改变商品。

代价：

- 模板修改不自动回写已生成的 SKU（历史 SKU 保持不变，避免价格/编码被暗中改写）；
- 商品可以没有模板（手工 SKU 仍然允许）。

## 兼容性

- 旧接口 `POST /admin/v1/products/{id}/sku-matrix[/-multi]` 保留（`SkuMatrixMultiService` 增加带 currency 的重载，模板场景首次建 SKU 时无需"从既有 SKU 推断币种"）；
- `options_json` 仍是投影，真相来源仍为 `sku_option_value`（ADR-003 不变）。

