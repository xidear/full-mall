# 目标完成清单（带证据）

> 目标：`goal-df88eca1`（自主完成所有此前反问/推迟的项）。
> 本文件只写"跑过什么命令、看到什么输出"，不做推测。

## 一、验证命令（可重复执行）

\`\`\`bash
cd /data/full-mall

make contract-check                 # 接口对齐 + 权限文档 + 兜底扫描
node scripts/acceptance-check.mjs   # 10 项端到端断言（需 API 在 41280）
docker run --rm -v /data/full-mall/server:/app -v /tmp/m2cache:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn -B \
  -Dtest=ConfigurableAssetStorageTest,ExportStorageGatingTest,CapabilityReadinessServiceTest test
npm run build -w @fullmall/admin    # 前端构建
\`\`\`

## 二、逐项证据

| # | 目标项 | 证据（命令 / 输出） | 提交 |
|---|---|---|---|
| ① | 全仓扫描静默兜底 + 接入 CI | `make contract-check` → `兜底检查通过（91 处已登记）`；未登记项会让 CI 失败 | `94b0be3` |
| ② | 能力就绪模型 | `GET /admin/v1/readiness` → 7 项，含 state/provider/missing/hint；冲突标 MISMATCH | `9e76725` |
| ③ | 导出存储去兜底 | 未配置创建任务 → `503 EXPORT_STORAGE_NOT_CONFIGURED`；配置后 → `202` | `9e76725` |
| ④ | StatusToggle 推广 | 供应商：停用/启用 200 + 审计 `SUPPLIER_STATUS_CHANGED`；优惠券：DRAFT→启用 200→停用 200→`DISABLED` | `a1b637a` `a3428ea` |
| ⑤ | 枚举字典覆盖 | 素材库用途下拉 = 商品图/规格图/视频封面/AI 生成/批量导入/自动化测试（后端下发）；POS 类型列中文 | `e11188f` |
| ⑥ | 三态测试 | `Tests run: 12, Failures: 0, Errors: 0`（storage 4 / export 3 / readiness 5） | `8aefd5d` |
| ⑦ | 生效值显示 | `GET /admin/v1/settings/effective?market=CN` → 12 项；页面渲染 12 行，来源 MARKET/GLOBAL | `1adf776` |
| ⑧ | 前端 locale | admin / store-console 均 `locale: zhCn`；admin 无英文 empty-text/placeholder；其余前端不用 Element Plus | `71aaeb9` `1adf776` |
| ⑨ | 构建 + 实测 + 提交 | 见下方"最近一次全量验证" | 本文件 |

## 三、最近一次全量验证（真实输出）

\`\`\`text
$ make contract-check
兜底检查通过（91 处已登记）

$ node scripts/acceptance-check.mjs
10/10 通过

$ mvn -Dtest=ConfigurableAssetStorageTest,ExportStorageGatingTest,CapabilityReadinessServiceTest test
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0

$ npm run build -w @fullmall/admin      → 0
$ npm run build -w @fullmall/storefront → 0
\`\`\`

## 四、未闭环项（结构性缺口，不是"没做"）

| 项 | 状态 | 原因 |
|---|---|---|
| ⑥ 的 AI 三态 | 未测 | `POST /admin/v1/ai/ask` **从未实现**（前端只有外壳）；为不存在的接口写"未配置返回 503"没有意义 |
| ④ 门店启停 | 未做 | admin 没有门店管理页，没有可挂载的列表 |
| ④ 促销启停 | 不做 | 促销/规则是 `DRAFT→ACTIVE→ENDED` 生命周期，布尔开关会把"草稿"表达成"停用"，语义错 |

这三项已在 `docs/SESSION_NOTES.md` 记录，等对应功能存在后再补。
