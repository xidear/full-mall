# 本批证据：门店管理 / 促销完善 / AI 真实模型 / 15052 重建

采集时间：2026-10-31（Asia/Shanghai）
环境：Linux 6.17 / Docker 29.9.2 / Node 22 / 无宿主 JDK（Java 走容器）

## 1. 数据库迁移

```text
docker exec fullmall-postgres-1 psql -U fullmall -d fullmall -c "select version,description,success from flyway_schema_history order by installed_rank"

 version | description | success
---------+-------------+---------
 1       | baseline    | t
 2       | seed        | t
 3       | store admin | t

       code      | market_code |    name    | active
-----------------+-------------+------------+--------
 STORE-22222222  | CN          | 上海静安店 | t
 STORE-44444444  | CN          | 北京朝阳店 | t
```

## 2. 后端定向测试（Docker Maven）

```text
mvn -B -q test -Dtest='StoreAdminServiceTest,StoreAccessServiceTest,AiAnswerServiceTest,KeywordRetrieverTest,PromotionServiceTest,PromotionAdminServiceTest,AdminPromotionAuthorizationTest'

StoreAdminServiceTest        Tests run: 6,  Failures: 0, Errors: 0
PromotionServiceTest         Tests run: 9,  Failures: 0, Errors: 0
AiAnswerServiceTest          Tests run: 11, Failures: 0, Errors: 0
KeywordRetrieverTest         Tests run: 4,  Failures: 0, Errors: 0
PromotionAdminServiceTest    Tests run: 8,  Failures: 0, Errors: 0
（StoreAccess/AdminPromotionAuthorization 同批通过）
```

## 3. 真实模型问答（不是拼片段）

```text
PUT /admin/v1/ai/providers/company-chat
{"kind":"TEXT","protocol":"openai-chat","endpoint":"https://company.binnarui.com:8500/v1",
 "model":"deepseek-v4.1-flash","apiKey":"not-required","enabled":true,"priority":10}
→ 200 {"updated":true,"code":"company-chat"}

GET /admin/v1/ai/status
→ {"ready":true,"chatProvider":"company-chat","chatModel":"deepseek-v4.1-flash",
   "retrieval":"KEYWORD","missing":[],"topK":5}

POST /admin/v1/faq {"question":"运费怎么算","answer":"满 99 元免运费，偏远地区另计。"} → 200

POST /admin/v1/ai/ask {"question":"运费怎么算"}
→ 200
{
  "answer": "满99元免运费，偏远地区另计。",
  "fallback": false,
  "citations": [{"sourceId":"c9c6ad16-…","score":0.95,
                 "snippet":"运费怎么算\n满 99 元免运费，偏远地区另计。"}],
  "topScore": 0.95,
  "provider": "company-chat",
  "retrieval": "KEYWORD",
  "latencyMs": 662
}
```

## 4. 门店 / 促销 HTTP 实测

```text
POST /admin/v1/stores                      → 201 code=STORE-VERIFY-707988 active=true
PUT  /admin/v1/stores/{id}                 → 200 name=验收临时门店（已改） pickup=false
POST /admin/v1/stores/{id}/active {false}  → 200 active=false
GET  /admin/v1/stores?includeInactive=true → 200 找到该行 active=false
（验收后按编码删除测试行：DELETE 1）

POST /admin/v1/promotions/{id}/duplicate   → 201 DEMO-FLASH-MUG-CC7CE DRAFT sold=0
PUT  /admin/v1/promotions/{id}             → 200 title=…（副本）（验收改）
POST /admin/v1/promotions/{id}/status?status=ACTIVE → 200 ACTIVE
POST /admin/v1/promotions/{id}/status?status=ENDED  → 200 ENDED
POST …status=ACTIVE（已结束再启用）        → 409 PROMOTION_STATUS_TRANSITION_INVALID
POST …status=NOPE                          → 400 PROMOTION_STATUS_INVALID
（验收后按编码删除测试行：DELETE 1）
```

## 5. 验收脚本

```text
node scripts/acceptance-check.mjs
… 14 项全部 PASS …
14/14 通过
```

新增覆盖：门店管理端点（编码/市场/状态 + 启停）、满减规则 `scopeIds`、AI 状态就绪判定与检索方式。

## 6. 浏览器真机（Playwright，生产构建）

```text
login ok http://127.0.0.1:41281/
/stores ok
/promotions ok
/ai ok
console errors: none
```

截图（`docs/evidence/`）：

- `stores-admin.png`：门店列表、市场、自提、内联状态开关、编辑
- `promotions-admin.png`：满减规则筛选 + 促销活动（类型/状态筛选、结束、复制为草稿）
- `ai-providers.png`：状态（deepseek-v4.1-flash / KEYWORD / 就绪）+ 模型供应商表

## 7. 15052 重建

```text
API 容器重启 → Flyway V3 成功 → Tomcat started on port 41280
admin：`scripts/admin-preview.sh restart` → `npm run build`（vue-tsc 0 错误）+ `vite preview` 0.0.0.0:41281
构建产物改为 /ui/**（避免与 API 的 /assets/** 冲突导致白屏）

GET http://company.binnarui.com:15052/                 → 200（生产构建 HTML）
GET http://company.binnarui.com:15052/admin/v1/ai/status → 401（同源代理到 API，鉴权正常）
```

## 8. 规范闸门

```text
make contract-check
  后端路由 435 条；前端调用 216 条；未匹配 0 条
  权限说明已是最新：15 条，接口映射 22 条
  兜底检查通过（93 处已登记）
  枚举检查通过（15 处已登记）

python3 scripts/check-help-coverage.py
  帮助检查通过：41 个页面帮助，索引已同步

npm run build --workspace @fullmall/admin
  ✓ built in ~15s（vue-tsc 0 错误）
```

## 9. 未验证 / 已知边界

- SEMANTIC 检索未验证：本机没有可用的 `openai-embedding` 供应商；KEYWORD 模式已真实验证。
- 15052 的 TLS/网关层未改动，只验证了 HTTP 直连映射。
- 旧版本（V1/V2 squash 之前）的数据库仍不支持原地升级，需按部署文档重建。