# 授权、试用与打包（设计规格 v1）

## 0. 交付形态决策（2026-10-19 确认）

| 决策 | 内容 |
|---|---|
| 许可形式 | **本地证书优先**（`license.json`，Ed25519 签名），公网许可服务端**后置**，需要在线吊销时再做 |
| 到期行为 | **只读**：GET 全部可用，**POST/PUT/PATCH/DELETE 一律 402**（不能增删改，只能查看） |
| 例外放行 | 登录/验证码、许可状态与上传、**支付回调** —— 用户已确认（2026-10-19）；否则交不了续费、或把已付款订单丢掉 |
| 社区版 | `app.license.enabled=false`，不校验、不阻断 |


> 目标：生产包支持**域名绑定**与**时间限制**；默认永久，可设 7 天 / 1 个月试用；
> 到期提示买断或续费；配套一个**许可验证服务端**。

## 1. 两件事必须分开

| 概念 | 作用 | 能否阻止盗用 |
|---|---|---|
| **许可证（License）** | 声明客户、域名、版本、功能、到期时间，私钥签名 | 提高合规门槛，**不能**阻止反编译改字节码 |
| **许可服务端** | 在线激活/续费/吊销/用量上报 | 可吊销、可统计，但离线部署必须允许宽限 |

诚实结论：Java 产物可被反编译修改，**任何本地校验都不是强 DRM**。
因此策略是：**合规成本低、被绕过可发现、核心价值在服务端（AI 训练/知识库更新/升级包）**。

## 2. 许可证格式

```json
{
  "licenseId": "LIC-2026-000123",
  "customer": "示例客户",
  "edition": "ENTERPRISE",
  "features": ["ai", "supplier-portal", "multi-store"],
  "domains": ["shop.example.com", "*.example.com"],
  "issuedAt": "2026-10-19T00:00:00Z",
  "expiresAt": "2027-10-19T00:00:00Z",
  "graceDays": 14,
  "instanceId": "可选：绑定首次激活的实例指纹"
}
```

- 签名：**Ed25519**（JDK 原生支持），私钥仅在许可服务端与打包机；公钥内置于应用；
- 传输：`license.json` + `license.sig`（或单文件 base64），后台可上传更新；
- 校验时机：启动时 + 每小时 + 关键写操作前（缓存结果，避免每请求验签）。

## 3. 域名绑定

- 配置 `app.license.domain-check=true`；取 `Host` 头（去掉端口）与 `domains` 通配匹配（`*.example.com` 支持一级通配）；
- 不匹配时：**不锁数据**，进入「受限模式」——后台可见横幅+许可页，前台返回维护页；
- 反向代理场景需 `server.forward-headers-strategy=framework` 且只信任已知代理 IP（防 Host 伪造）；
- 本地 `localhost/127.0.0.1` 永远放行（便于运维与演示）。

## 4. 试用与到期

| 打包预设 | 参数 | 行为 |
|---|---|---|
| 永久（默认） | `expiresAt=null` | 无到期提示 |
| 试用 7 天 | `-Ptrial=7` | 打包时写入签发时间，`expiresAt=+7d` |
| 试用 30 天 | `-Ptrial=30` | 同上 |

到期行为（**不破坏数据、可恢复**）：

1. 启动仍成功（与"缺配置不阻断启动"一致），日志 WARN + 后台横幅；
2. 业务写接口返回 `402 LICENSE_EXPIRED`（含续费说明），**读与导出保持可用**；
3. AI 等付费功能停用；
4. 上传新许可证后立即恢复，无需重启（定时任务 + 上传即生效）。

时间防篡改：记录"最后运行时间"（`license_state.last_seen_at`），
若系统时间早于该值超过 24 小时 → 视为时钟回拨，进入受限模式并记录审计（**不静默继续**）。

## 5. 许可服务端（独立服务）

接口（`/api/v1/licenses/**`，mTLS 或 API Key）：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/activate` | 首次激活：domain + instanceId → 签发许可证 |
| POST | `/renew` | 续费：延长 `expiresAt`（支付回调后调用） |
| POST | `/heartbeat` | 心跳：上报版本/域名/用量，返回吊销状态 |
| GET | `/public-key` | 公钥（供离线打包校验） |

- 数据库：`license`（客户、域名、版本、到期、状态）、`license_event`（激活/续费/吊销/心跳）；
- 吊销列表：心跳或每日检查时拉取；离线部署宽限 `graceDays`；
- 服务端自身部署在**公网**（客户内网无法访问时走离线许可证文件）。

## 6. 打包与产物

| 产物 | 内容 | 校验 |
|---|---|---|
| `fullmall-community-<ver>.jar` | 无 ai / 无许可强制 | 启动打 `edition=COMMUNITY` |
| `fullmall-enterprise-<ver>-trial7.jar` | 含 ai，试用 7 天 | 启动日志 + 后台许可页显示到期 |
| `fullmall-enterprise-<ver>-permanent.jar` | 含 ai，永久 | 同上 |

- 打包脚本 `deploy/build-license.sh`：生成密钥对（离线保管）、签许可证、注入公钥、产出三件套；
- **私钥绝不入库**；`.gitignore` 覆盖 `*.pem`、`license-private*`；
- 构建产物附 SHA-256 与签名，交付单列明。

## 6.1 公开分叉导出规则（重要）

企业版源码保存在 Gitea 的 `server/src-enterprise/`，**不得进入 GitHub/Gitee 公开分叉**。
导出公开版本时必须执行（脚本 `scripts/export-community.sh` 待实现）：

```bash
rm -rf server/src-enterprise
rm -f docs/LICENSING_AND_BUILD.md docs/AI_SERVICE_MODULE.md
# 然后按 community profile 构建并核对 jar 内无 com/fullmall/ai
```

验收：公开分叉 `git grep -l "com.fullmall.ai"` 结果为空，且构建产物 `/admin/v1/ai/**` 404。

## 7. 验收标准

| 编号 | 验收 |
|---|---|
| LIC-1 | 无许可证时启动成功，后台显示「未授权/试用中」，前台可用（默认永久版除外） |
| LIC-2 | 域名不匹配 → 受限模式，前台维护页，后台可见原因 |
| LIC-3 | 到期后写接口 402、读与导出可用；上传新证即刻恢复（不重启） |
| LIC-4 | 时钟回拨 25 小时 → 受限模式 + 审计记录 |
| LIC-5 | 伪造许可证（改 domains/expiresAt）→ 验签失败并明确拒绝 |
| LIC-6 | 社区版产物 `/admin/v1/ai/**` 404，且 jar 内无 `com/fullmall/ai` 类 |

## 8. 不做的事（避免过度承诺）

- 不做代码混淆/加固（可加 ProGuard 但收益有限，且破坏可维护性）；
- 不做硬件指纹硬绑定（换服务器即失效，客户体验差）；
- 不把 AI 模型打进 jar（体积与许可问题），模型由部署脚本拉取。
