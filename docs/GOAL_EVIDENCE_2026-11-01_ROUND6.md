# 第六轮证据：自动汇率 mock 语义修正

## 1. 用户纠正

> 既然 `https://api.frankfurter.dev/v1` 能正常使用，为什么要用 mock？我是说三方服务未配置才用 mock。

上一轮实现把 `dev` 与 `demo` 一并按“未配置用 mock”处理，且我在真机测试后把运行环境恢复成了
`mock`。两处都错：**能用真实源时不该用固定值糊弄**。

## 2. 修正后语义

```text
resolveProviderCode()：
  已配置供应商（exchange_rate）      → 用配置的那个（frankfurter / mock）
  未配置 + app.mode=demo            → mock 兜底（演示）
  未配置 + app.mode=dev / prod      → 返回 null：不自动拉取，用手工汇率
```

`demoMock` 只在「未配置任何供应商 + demo 模式」为真；显式选 mock 不算演示兜底。

## 3. 运行环境事实（已改回真实源）

```text
integration_config：exchange_rate/frankfurter enabled=t；mock enabled=f
GET  /admin/v1/exchange-rates/provider
→ {"configured":true,"provider":"frankfurter","demoMock":false,...}
POST /admin/v1/exchange-rates/refresh
→ 200 写入 source=frankfurter 的汇率（CNY→AUD 0.21398 等）
```

`app.mode=dev`（`.env`），未配置也不会自动拉取——现在配置了，所以用真实汇率。

## 4. 测试

新增 `ExchangeRateFetchModeTest`（4 项）：

```text
unconfiguredDevDoesNotFetch        → configured=false, provider=null, demoMock=false
unconfiguredDemoUsesMock           → provider=mock, demoMock=true
configuredProviderWinsOverDemoDefault
                                   → provider=frankfurter, demoMock=false
refreshWithoutConfigWritesNothing  → prod 未配置时不写任何汇率（verifyNoInteractions）
```

`ExchangeRateFetchServiceTest` 用 `app.mode=demo` 验证 mock 兜底路径仍然可用。

## 5. 文档同步

- ADR-0017：改为“只有 demo 未配置才用 mock；dev/prod 未配置不自动拉取”
- 依赖矩阵、ROUND5 证据、SESSION_NOTES、admin 帮助与 `HELP_INDEX` 同步
- 全仓已无 `演示/开发未配置用 mock` 与 `demo/dev` 旧表述

## 6. 构建环境修复（顺带）

`server/target` 里残留 278 个 **root 属主** 的 class（早前一次以 root 用户跑过 Maven 造成），
导致非 root 容器覆盖写入失败：

```text
error while writing ...ArticleService$ArticleView.class
```

用 root 容器 `rm -rf server/target` 清理后重跑。结论：**Maven 容器必须带 `-u $(id -u):$(id -g)`**。

## 7. 回归

- 后端：`mvn clean test` 全量结果见交付报告
- Playwright：全量结果见交付报告