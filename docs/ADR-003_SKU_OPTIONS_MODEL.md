# ADR-003：规格用关联表，不用 JSON 字段

- 状态：**已决定（采纳关联表）**
- 背景：SKU 规格当前存在 `sku.options_json`（JSONB），生成与改价都靠 `options_json @>`。
  业务方指出"以前都是用关联表"。理论维度无限，实际常用 2–3 维（当前限制 4 维是合理的保护）。

## 1. 两种方案对比

| 维度 | JSONB（现状） | 关联表（建议） |
|---|---|---|
| 写入灵活性 | 加维度不用改表 | 加维度只是插数据，同样无需改表**结构** |
| **取值合法性** | ❌ 任何字符串都能写进去（"256G" vs "256g" vs "256 G"） | ✅ 值来自 `product_option_value`，天然不允许野值 |
| **唯一性** | ❌ 无法阻止同一商品出现两个 `{颜色:黑,存储:256G}` 的 SKU | ✅ 可建唯一约束（商品 + 值组合） |
| **报表/统计** | ⚠️ 要解 JSON，按值分组麻烦 | ✅ 普通 JOIN + GROUP BY，财务/运营口径统一 |
| **筛选性能** | ✅ GIN 索引，`@>` 很快 | ✅ 索引覆盖，同量级（组合行数 = SKU 数 × 维数，很小） |
| **改规格值**（把"256G"改成"256GB"） | ❌ 要批量改 JSON，容易漏 | ✅ 改一行 `option_value.name`，所有 SKU 同步 |
| **引用完整性** | ❌ SKU 删了/值删了没有约束 | ✅ 外键约束，删除前必须处理引用 |
| **前端读取** | ✅ 直接拿一个字段 | ⚠️ 需要聚合查询（一次 JOIN 即可） |
| **调试直观性** | ✅ `psql` 一眼看懂 | ⚠️ 要看多表（可用视图解决） |
| 迁移成本 | 0 | 一次性迁移 + 改 3 处代码 |

**核心差异不是性能，而是「正确性」**：JSON 允许写入非法值、允许重复组合、没有引用完整性。规格一旦错，改价、库存、促销范围全跟着错。

## 2. 决定的模型（关联表）

```sql
-- 商品的规格维度（颜色、存储…），每个商品一套
product_option(id, product_id, name, sort_order)          UNIQUE(product_id, name)
-- 维度取值（黑/白、256G/512G…）
product_option_value(id, option_id, name, sort_order)     UNIQUE(option_id, name)
-- SKU 与取值的对应（一个 SKU 每个维度恰好一个值）
sku_option_value(sku_id, option_value_id)                 UNIQUE(sku_id, option_id)
                                                          -- course: option_id 需冗余存以便唯一约束
```

- **每个 SKU 在每个维度上只能有一个值**：由 `UNIQUE(sku_id, option_id)` 保证；
- **同一商品下不允许重复组合**：用"组合签名"列或在应用层校验（推荐加 `sku.combination_key` 唯一约束，
  由值 id 排序拼接生成，数据库层强制）；
- 维度数上限仍保留 **4**（实际 2–3），超限明确报错。

## 3. 对现有代码的影响（3 处）

| 位置 | 现在 | 改为 |
|---|---|---|
| 多维生成 `SkuMatrixMultiService` | 写 `options_json` | 建维度/取值行 + 写 `sku_option_value` + 组合签名 |
| 按规格改价 `SkuPriceRuleService` | `options_json @> {...}` | `EXISTS (SELECT 1 FROM sku_option_value v JOIN … WHERE v.name = ?)` |
| 商品详情/可用量 | 读 `options_json` 展示 | 一次 JOIN 聚合（或建 `v_sku_options` 视图） |

## 4. 迁移步骤（渐进，不破坏线上）

1. 建三张表 + `sku.combination_key`（可空）；
2. 写迁移脚本：把现有 `options_json` 的键值灌入关联表（幂等）；
3. 双写：生成/编辑同时写关联表与 JSON（JSON 暂时保留）；
4. 改读取与筛选走关联表；
5. 全量测试通过后**删除** `options_json` 与双写（一次性收尾）。

> 不做"永远双写"：两份真相迟早不一致。

## 5. 结论

**采纳关联表**。理由排序：
1. 数据正确性（禁止野值、禁止重复组合、可强制引用完整性）；
2. 运营/财务报表可直接按规格聚合；
3. 改规格值是单行更新，不用批量改 JSON。

JSON 只在"只读投影/前端缓存"场景保留价值，不作为真相来源。
