# ADR-005：第三方 API 配置统一放后台

- 状态：已决定，待实现
- 问题：目前第三方凭据（短信、支付、存储、审核、物流、AI）只走环境变量，
  运营看不到、改不了、换供应商要重启并改部署配置。

## 1. 结构：一张表 + 一个页面

```
integration_config
  capability     VARCHAR(32)   -- sms / payment / storage / moderation / logistics / ai
  provider       VARCHAR(32)   -- aliyun / tencent / wechat / alipay / qiniu / local ...
  enabled        BOOLEAN
  config_enc     BYTEA         -- AES-GCM 加密后的 JSON（含 accessKey/secret/模板ID…）
  config_hint    JSONB         -- 非敏感信息明文（region、bucket、签名名、模板ID 等）
  updated_by     VARCHAR(64)
  updated_at     TIMESTAMPTZ
  PRIMARY KEY (capability, provider)
```

- **敏感字段**（secret/key/token/私钥）只进 `config_enc`，**永不回显**；
- `config_hint` 存"可以给人看"的部分（如 region、bucket、sign name），页面用来展示"已配置什么"；
- 每类能力**同时只有一个 enabled**（切换服务商 = 换 enabled 行），由唯一索引保证。

## 2. 页面：系统设置 → 第三方服务

一个页面按能力分区，每区包含：

| 元素 | 说明 |
|---|---|
| 服务商下拉 | 只列"有适配器实现"的（见 ADR-004 清单），不列未实现的 |
| 凭据表单 | 按服务商动态显示字段（阿里云：AK/SK/签名/模板；腾讯：SecretId/Key/SdkAppId…） |
| 状态徽标 | 未配置 / 已配置未验证 / 已验证 / 调用失败 |
| 「测试连接」 | 调一次最轻量的 API（短信查签名、存储列 bucket、审核一条样例文本） |
| 保存 | 二次确认 + 审计事件 `INTEGRATION_UPDATED` |

## 3. 安全规则（硬性）

1. **加密**：AES-256-GCM，主密钥来自环境变量 `APP_SECRET_KEY`（**不入库、不入仓库**）；
   主密钥缺失时后台保存直接报错，不允许明文落库。
2. **只写不读**：任何接口都不返回 secret；编辑时显示 `••••` + "重新输入以更换"。
3. **审计**：每次更新写 `INTEGRATION_UPDATED`（谁、哪个能力、哪个服务商、改了哪些字段名，**不含值**）。
4. **优先级**：数据库配置 > 环境变量 > 默认（local/mock）。数据库没配时仍可用环境变量，二者不冲突。
5. **切换即生效**：保存后重建该能力的 Bean（`@RefreshScope` 或工厂），不需要重启进程。
6. **权限**：仅 `ADMIN`；查看"是否已配置"可给 `OPERATOR`。

## 4. 与 ADR-004 的关系

适配器仍是独立包、可删除；本 ADR 只解决"**配置从哪来**"。
适配器构造函数不变，改由 `IntegrationConfigProvider` 把解密后的配置喂进去。

## 5. 实施顺序

| 步 | 内容 | 可验证点 |
|---|---|---|
| 1 | 建表 + 加密工具（AES-GCM，主密钥缺失即失败） | 加解密单测、主密钥缺失报错 |
| 2 | 配置读写服务 + 审计 | 写入后读回 hint、secret 不可读 |
| 3 | 各适配器改由配置提供者装配 | 现有 casing 测试不变 |
| 4 | 后台页面（分区表单 + 测试连接） | 页面构建 + E2E |
| 5 | 文档与运维说明 | — |

## 6. 明确不做

- 不在前端做加密（防不住，且前端拿不到主密钥）；
- 不把 secret 写进日志/审计明文；
- 不做"多套配置同时启用"（一个能力一个生效配置，避免行为不确定）。
