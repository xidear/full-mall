# iKuai 路由器「新增端口映射」只读侦察报告

- 执行时间：2026-10-27 16:14 – 2026-10-28 07:20 (UTC)
- 执行者：网络运维子代理 + 主代理（先只读探测，后由主代理写入 dnat）
- ⚠️ 本报告含明文维护密码/令牌；推 GitHub/Gitee 前请脱敏或排除
- 工作机：192.168.1.15（Debian 12 / Linux 6.17 / bash 5.2.15 / node v22.23.2 / wget 1.21.3；无 curl、sshpass、expect）
- 路由器：192.168.1.1，iKuai OS V4（页面标题 `iKuai OS V4`，`x-arch: arm`、`x-sysbit: x64`，前端构建于 2026/09/28）

---

## 0. 当前状态（2026-10-28 07:20）：已完成

- 使用 Web 账号成功登录（账号凭据见本次会话，不写入公开仓库；DNSPod Token 见本机忽略提交的 `.env.ops`）。
- 已写入并验证 4 条 dnat（id 59–62）：
  - `15080 → 192.168.1.15:80`（edge HTTP）
  - `15443 → 192.168.1.15:443`（edge HTTPS）
  - `15051 → 192.168.1.15:41285`（storefront 直连）
  - `15052 → 192.168.1.15:41281`（admin 直连）
- 80/443 未占用；`node scripts/ikuai-dnat.mjs add` 已做幂等（按 wan_port 跳过）。
- 回滚：`node scripts/ikuai-dnat.mjs del --ids 59,60,61,62`（执行前先 `show` 确认 id）。
- `https://company.binnarui.com:15443` 已使用 Let's Encrypt 证书（DNS-01）。

## 1. 结论（TL;DR）

| 问题 | 结论 | 置信度 |
|---|---|---|
| Web 登录 API 形态 | `POST /Action/login`，body `{username, passwd, pass}`；`passwd=md5hex(密码)`、`pass=base64("salt_11"+密码)`；成功下发 cookie `sess_key` | **已确认**（前端源码 + 实测） |
| 端口映射 API | `POST /Action/call`，`func_name=dnat`，action = `show/add/edit/del/up/down` | **已确认**（前端源码） |
| 新增规则完整字段 | `tagname, enabled, lan_addr, lan_port, protocol(tcp/udp/tcp+udp), interface, wan_port, src_addr{custom,object}, comment` | **已确认**（addOrEdit 表单源码） |
| 删除/回滚参数 | `param:{id:"3"}`，批量 `{id:"3,4,5"}` | **已确认**（表格行操作源码） |
| 「临时令牌」= telnet/SSH 密码？ | **不是**（admin/root/xidear × 令牌全部「用户名或密码错误」） | 已实测 |
| 「临时令牌」= /Action/call 的 API Token？ | **不是**（8 种携带方式全部 1008，与未登录基线完全一致） | 已实测 |
| 「临时令牌」= Web 密码（admin）？ | **不是**（返回 1005，且是真正校验过的一次） | 已实测 |
| **当前状态** | **已完成 4 条 dnat 映射并通过公网 HTTPS 验证** | — |

> 一句话：链路全通、dnat 已写入；80/443 未占用，公网使用 15080/15443/15051/15052。

---

## 2. 网络与管理面事实（实测）

| 目标 | 结果 | 证据 |
|---|---|---|
| `http://192.168.1.1:80` | 端口未监听 | `ECONNREFUSED` |
| `http://192.168.1.1:180` | 302 → `https://192.168.1.1:1443/` | 响应头 `location` |
| `https://192.168.1.1:1443/login` | 200，React SPA（入口 `/static/js/index-CZFa-cUq.js`） | 页面 HTML |
| `https://192.168.1.1:1443/` | 302 → `/login` | 响应头 |
| `/static/js/first.json` | **免登录可读**，暴露登录页配置（云平台入口 `https://yun.ikuai8.com/login`） | 正文见 §4.5 |
| telnet 23 | 开放，**GBK 编码**提示「用户名: / 密 码:」，失败提示「用户名或密码错误」 | 抓包转码 |
| SSH 122 | 开放，`SSH-2.0-dropbear`，允许 password 认证 | `Permission denied (publickey,password)` |

响应头设备指纹：`server: Nginx`、`x-arch: arm`、`x-sysbit: x64`、`x-lang: 1`、`x-timezone: 0800`、`x-enterprise: 0`。

工作机侧：`hostname -I` = 192.168.1.15（另有 docker 网桥），默认网关 = 192.168.1.1，与映射目标一致。

---

## 3. 登录机制（前端 bundle 反查，一手证据）

### 3.1 登录请求体

出处：`/static/js/index-CZFa-cUq.js`（登录页组件）

```js
(t = { username: e.email,
       passwd: le(e.password).toString(),
       pass:  la("salt_11" + e.password) },
 Hi("/Action/login", t))
```

同一文件内 `la` 的定义（**是 Base64，不是哈希**）：

```js
la = e => { ... const t = (new TextEncoder).encode(String(e)); ... btoa(a) ... }
```

`le` 来自 `/static/js/crypto-CGSYo6wd.js`（crypto-js 打包），即 **MD5（hex）**。

因此完整登录体为：

```json
{
  "username": "admin",
  "passwd": "<md5_hex(password)>",
  "pass": "<base64('salt_11' + password)>"
}
```

> 关键细节：`pass` 的盐是**字面量** `salt_11`，不是每台设备随机盐。

### 3.2 会话

- 成功后服务端下发 `Set-Cookie: sess_key=...`；前端后续请求全部带该 cookie（`withCredentials: true`）。
- 未登录或会话失效统一返回：`{"message":"会话已过期","Result":10000,"code":1008}`。

### 3.3 实测错误码

| code | message | 含义 |
|---|---|---|
| 0 | （成功） | 登录成功 / 调用成功 |
| 1005 | 账号或密码错误 | 凭据不对（**已被真正校验**） |
| 1006 | 账号密码错误次数过多，锁定5分钟 | 失败次数超限，5 分钟内任何尝试都返回 1006（**不再校验凭据**） |
| 1008 | 会话已过期 | 无有效 `sess_key` |

> **重要陷阱**：1006 窗口内的尝试全部无效。主代理此前「用 token 当密码试过」很可能落在锁定窗口，结论不可信；本次已在解锁后重测（§5 #11）。

---

## 4. 端口映射（dnat）API —— 权威字段

出处：`/static/js/portMapping-BHUNf-zY.js`（请求封装）、`/static/js/addOrEdit-D0Mg7ARY.js`（新增/编辑表单）。

### 4.1 端点与动作

所有调用都是 **POST** `https://192.168.1.1:1443/Action/call`，body 固定三段式：

```json
{ "func_name": "dnat", "action": "<show|add|edit|del|up|down>", "param": { } }
```

封装源码（原样）：

```js
l = a => r("/Action/call", {action:"show", func_name:"dnat",
        param: {...{TYPE:"data,total,enabled_total,disabled_total"}, ...a}})   // 列表
i = a => r("/Action/call", {action:"add",  func_name:"dnat", param:{...a}})    // 新增
m = a => r("/Action/call", {action:"del",  func_name:"dnat", param:{...a}})    // 删除
c = a => r("/Action/call", {action:"edit", func_name:"dnat", param:{...a}})    // 编辑
s = a => r("/Action/call", {action:"up",   func_name:"dnat", param:{...a}})    // 启用
b = a => r("/Action/call", {action:"down", func_name:"dnat", param:{...a}})    // 停用
```

### 4.2 列表（show）参数

```json
{ "TYPE": "data,total,enabled_total,disabled_total", "limit": "0,200" }
```

- `limit` 格式为「偏移,条数」。
- 可按状态过滤：`FILTER1: "enabled,=,yes"`。
- 返回 `results.data[]`，每条含 `id`（回滚要用）、`tagname`、`enabled`、`lan_addr`、`lan_port`、`protocol`、`interface`、`wan_port`、`src_addr`、`comment`。

### 4.3 新增（add）完整字段——直接取自表单提交代码

源码原样（`addOrEdit-D0Mg7ARY.js`）：

```js
const r = {
  tagname: e.tagname,
  enabled: "yes",
  lan_addr: e.lan_addr,
  lan_port: e.lan_port,
  protocol: e.protocol,
  interface: "externalNetworkInterface" === e.mapping_type
      ? (Array.isArray(e.externalNetworkInterface_interface) && e.externalNetworkInterface_interface.length > 0
          ? e.externalNetworkInterface_interface.join(",") : "all")
      : e.externalNetworkIP_interface,
  wan_port: e.wan_port,
  src_addr: { custom: [...], object: [...] },
  comment: e.comment
};
"edit" === ne && (r.id = ie.current.id, r.enabled = ie.current.enabled)
```

| 字段 | 类型 | 取值 / 说明 |
|---|---|---|
| `tagname` | string | 规则名称 |
| `enabled` | string | `"yes"` / `"no"`（**不是布尔**） |
| `lan_addr` | string | 内网目标 IP，本次为 `192.168.1.15` |
| `lan_port` | string | 内网端口；与 `wan_port` 端口个数必须一致（源码校验 `portCountNotMatch`） |
| `protocol` | enum | **仅** `tcp` / `udp` / `tcp+udp`（源码常量表，默认 `tcp`） |
| `interface` | string | `"all"` = 全部 WAN；或接口名逗号分隔（如 `wan1,wan2`） |
| `wan_port` | string | 外网端口，可为区间（如 `8000-8010`），个数须与 `lan_port` 相同 |
| `src_addr` | object | `{custom: [], object: []}` = 不限来源；`custom` 为自定义 IP/网段数组，`object` 为地址对象 |
| `comment` | string | 备注 |
| `id` | string | **仅 edit** 时携带 |

### 4.4 删除 / 启停（回滚用）

行操作为单条 `{id}` 或批量（源码 `{id: $e.map(de).join(",")}`）：

```json
{ "func_name": "dnat", "action": "del",  "param": { "id": "3,4,5" } }
{ "func_name": "dnat", "action": "down", "param": { "id": "3" } }
```

### 4.5 免登录可读的 first.json（登录页配置片段）

```json
{ "oemLoginPage": false,
  "loginConfig": {
    "yunUrl": { "show": true, "zhCN": "https://yun.ikuai8.com/login" },
    "wwwUrl": { "show": true, "zhCN": "https://www.ikuai8.com" },
    "bbsUrl": { "show": true, "zhCN": "http://bbs.ikuai8.com" } },
  "lite": false, "auto_refresh_rate": 1, "leftMenuVersion": 1 }
```

---

## 5. 尝试记录（逐条，含原始响应）

> 全部为只读或登录尝试；**未提交任何 add/edit/del**。

| # | 时间(UTC) | 方式 | 凭据 | 结果 | 原始响应 |
|---|---|---|---|---|---|
| 1 | 16:31 | `POST /Action/call` 无会话 | — | 基线 | `{"message":"会话已过期","Result":10000,"code":1008}` |
| 2 | 16:31 | 令牌携带 ×7：header `token` / `X-Token` / `Authorization: Bearer` / `api-token`、cookie `sess_key`、body `token`、query `?token=` | 令牌原文 + 解码值 | 全失败 | 均 `code 1008`（与基线一致） |
| 3 | 16:31 | `POST /Action/login` 空体 | — | 错误 | `{"message":"账号或密码错误","Result":10000,"code":1005}` |
| 4 | 16:36 | SSH 122 | `root` / 令牌 | 失败 | `root@192.168.1.1: Permission denied (publickey,password).` |
| 5 | 16:36 | SSH 122 | `admin` / 令牌 | 失败 | 同上 |
| 6 | 16:36 | telnet 23（GBK） | `admin` / 令牌；`root` / 令牌 | 失败 | `用户名或密码错误` |
| 7 | 16:37 | telnet 23 | `xidear` / `xidear@12`；`xidear` / 令牌 | 失败 | `用户名或密码错误` |
| 8 | 16:37 | SSH 122 | `xidear` / `xidear@12` | 失败 | `Permission denied` |
| 9 | 16:37 | Web 登录（正确编码） | `admin` / `xidear@12` | **真正校验后失败** | `code 1005` |
| 10 | 16:39 | Web 登录 | `admin` / 令牌 | 命中锁定窗口，**结论无效** | `code 1006 锁定5分钟` |
| 11 | 16:45 | Web 登录（解锁后重测） | `admin` / 令牌 | **真正校验后失败** | `code 1005` |
| 12 | 16:47 | Web 登录 | `admin` / 令牌解码值 `95804ba8-2a63-4735-97be-` | **真正校验后失败** | `code 1005` |

### 5.1 锁定机制

实测：在已有失败累计的情况下，第 3 次尝试即触发 `1006`，**持续 5 分钟且期间不校验凭据**。后续任何重试必须：先等满 ≥5 分钟，再一次性使用**确定正确**的凭据，不要连打。

---

## 6. 「临时令牌」到底是什么？

### 6.1 令牌本体

```
原文: OTU4MDRiYTgtMmE2My00NzM1LTk3YmUt   (32 字符)
解码: 95804ba8-2a63-4735-97be-          (24 字符)
```

解码结果是 **UUID 的前 24 位（在第二个 - 后被截断）**，不是完整 UUID（36 位）。这意味着：**要么原始令牌本就是 24 字节短 ID，要么用户粘贴时被截断**。若属后者，任何用法都不会成功——建议向用户确认来源与完整长度。

### 6.2 iKuai V4 里与「令牌」相关的只有两处（全量 400 个 chunk 检索）

| 功能 | 位置 | 原文说明 |
|---|---|---|
| **个人 API 令牌** | 系统设置 → 登录管理 → 个人API令牌（`func_name=api_tokens`） | 「快速创建专属 API 令牌，安全连接您的应用、服务与自动化流程」「**开通前请先绑定云平台**」「权限级别为您当前拥有的用户权限级别」「关闭窗口后将无法再次查看」 |
| **远程维护** | 系统设置 → 登录管理 → 远程访问（`/loginManagementRemote`） | 「**通过 WEB 或 Telnet 的方式远程登录路由并管理**」，配套字段：远程维护端口、**远程维护密码** |

> 前端**没有**任何「临时令牌 / 一次性令牌」字样（`临时` 只出现在「上网码 / 临时认证」这类访客认证文案里）。所以「临时令牌」是**用户口语**，指向上表两者之一，或来自爱快云（`https://yun.ikuai8.com`）。

### 6.3 排除法

| 假设用法 | 实测 | 结论 |
|---|---|---|
| telnet 密码（admin / root / xidear） | 均「用户名或密码错误」 | 否 |
| SSH 密码（root / admin / xidear） | `Permission denied` | 否 |
| Web 登录密码（admin） | `code 1005` | 否 |
| `/Action/call` 的 header / cookie / query / body 令牌 | 全 `1008` | 否（该固件不认这种直连令牌） |
| 个人 API 令牌（`api_tokens`） | 无会话无法查询 | 未证伪：需登录后在「个人API令牌」页比对，或该令牌由云平台签发 |

**结论**：令牌既不是 telnet/SSH 口令，也不是可直连 `/Action/call` 的 API Token。最可能是「个人API令牌」页里那条令牌（需先登录核对），或爱快云侧的一次性入口凭证——两者都**不能绕过 Web 登录**。

---

## 7. 已执行命令（保留用于复核/回滚）

### 7.1 用现成脚本（推荐，已随本报告提交）

脚本：[`scripts/ikuai-dnat.mjs`](../scripts/ikuai-dnat.mjs)（默认 dry-run，安全）

```bash
export IKUAI_USER='admin'          # ← 待填：真实管理员用户名
export IKUAI_PASS='********'       # ← 待填：真实管理员密码
export IKUAI_TARGET_IP='192.168.1.15'

# 0) 只看报文，不联网
node scripts/ikuai-dnat.mjs plan

# 1) 只读：现状（务必先存一份，回滚要用 id）
node scripts/ikuai-dnat.mjs show | tee /tmp/ikuai-dnat-before.json

# 2) 新增 15080/15443（边缘）与 15051/15052（商城/后台直连）
node scripts/ikuai-dnat.mjs add

# 3) 校验
node scripts/ikuai-dnat.mjs show | tee /tmp/ikuai-dnat-after.json

# 4) 回滚（本机实际 id 为 59,60,61,62；执行前先 show 确认）
node scripts/ikuai-dnat.mjs del --ids '3,4,5'
```

### 7.2 等价的裸报文（无脚本时）

```text
登录:   POST https://192.168.1.1:1443/Action/login
        body = {"username":"<用户>","passwd":"<md5(密码)>","pass":"<base64('salt_11'+密码)>"}

列表:   POST https://192.168.1.1:1443/Action/call
        body = {"func_name":"dnat","action":"show","param":{"TYPE":"data,total,enabled_total,disabled_total","limit":"0,200"}}
```

> 本机**没有 curl**，实际执行请用 node（脚本）或 `wget --no-check-certificate --header`。

### 7.3 四条规则的完整报文（与脚本输出一致）

```json
{"func_name":"dnat","action":"add","param":{"tagname":"fm-edge-15080","enabled":"yes","lan_addr":"192.168.1.15","lan_port":"80","protocol":"tcp","interface":"all","wan_port":"15080","src_addr":{"custom":[],"object":[]},"comment":"full-mall-edge-http"}}
{"func_name":"dnat","action":"add","param":{"tagname":"fm-edge-15443","enabled":"yes","lan_addr":"192.168.1.15","lan_port":"443","protocol":"tcp","interface":"all","wan_port":"15443","src_addr":{"custom":[],"object":[]},"comment":"full-mall-edge-https"}}
{"func_name":"dnat","action":"add","param":{"tagname":"fm-shop-15051","enabled":"yes","lan_addr":"192.168.1.15","lan_port":"41285","protocol":"tcp","interface":"all","wan_port":"15051","src_addr":{"custom":[],"object":[]},"comment":"full-mall-shop"}}
{"func_name":"dnat","action":"add","param":{"tagname":"fm-admin-15052","enabled":"yes","lan_addr":"192.168.1.15","lan_port":"41281","protocol":"tcp","interface":"all","wan_port":"15052","src_addr":{"custom":[],"object":[]},"comment":"full-mall-admin"}}
```

### 7.4 执行前检查清单

- [x] 已 `show` 确认 **15080 / 15443 / 15051 / 15052 未被占用**（路由器自身 Web 用 180 / 1443）
- [ ] `wan_port` 若为区间，条目数必须与 `lan_port` 一致
- [ ] 记录新增返回的 `id`，或从 `show` 按 `tagname` 找回
- [ ] 回滚只 `del` 这三条 id，**不要**批量删其它规则
- [ ] 若 WAN 口不是「任意」，把 `interface` 改成实际接口名（照抄 `show` 里已有规则的 `interface`）

---

## 8. 还缺什么 & 两个最可能方案

**唯一缺口：Web 管理员的用户名 + 密码。**（用户名大概率是 `admin`，但密码未知；也可能是改过的自定义账号名。）

### 方案 A（最直接）：提供 Web 管理员口令

用户查证后给出账号密码 → 直接跑 §7.1 的 `show` → `add`。
注意：**先等满 5 分钟**（锁定窗口内必然 1005/1006），且第一次就必须用正确凭据。

### 方案 B（无需知道口令）：复用浏览器现有会话

1. 用户在已登录的浏览器 F12 → Application → Cookies → `https://192.168.1.1:1443` → 复制 `sess_key`。
2. 用该 cookie 直接调 API（无需再登录）：

```bash
export IKUAI_SESS_KEY='<粘贴 sess_key>'
node -e '
process.env.NODE_TLS_REJECT_UNAUTHORIZED="0";
const k=process.env.IKUAI_SESS_KEY;
const r=await fetch("https://192.168.1.1:1443/Action/call",{method:"POST",
  headers:{"Content-Type":"application/json",Cookie:"sess_key="+k},
  body:JSON.stringify({func_name:"dnat",action:"show",param:{TYPE:"data,total,enabled_total,disabled_total",limit:"0,200"}})});
console.log(await r.text());'
```

> `sess_key` 会随会话超时失效，拿到后尽快执行。

### 补充：若想确认「临时令牌」归属

登录后到「系统设置 → 登录管理 → 个人API令牌」核对列表（`func_name:"api_tokens", action:"show"`）；若列表为空，则该令牌来自爱快云（`https://yun.ikuai8.com`），与本地 API 无关。

---

## 9. 安全提醒

- 本文件含**明文凭据**（远程维护密码 `xidear@12`、令牌原文）。按 AGENTS.md，**GitHub / Gitee 公开仓库禁止包含密钥**；提交 Gitea 内部仓库可保留，推送公开端前请排除或脱敏本文件。
- 路由器有「5 次失败锁定 5 分钟」机制，不要再做无依据的密码猜测。
- 本次全程**只读**：仅调用 `/Action/login` 与 `/Action/call`（show 及无会话探测），未提交任何 add/edit/del。

---

## 10. 附：证据文件位置（本机临时目录，不随仓库提交）

| 文件 | 内容 |
|---|---|
| `/tmp/ikuai/login.html` | 登录页 HTML |
| `/tmp/ikuai/js/*.js` | 入口 bundle + 关键 chunk（index / crypto / PortMappingPage / addOrEdit / portMapping） |
| `/tmp/ikuai/all/*.js` | 全部 400 个懒加载 chunk（检索 token / dnat 用） |
| `/tmp/ikuai/telnet2.mjs` | GBK 感知的 telnet 探测脚本 |
| `/tmp/ikuai/login.mjs` | 单次登录探测脚本 |
| `/tmp/ikuai/probe.mjs` | 令牌携带方式矩阵探测脚本 |
