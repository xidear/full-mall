#!/usr/bin/env node
/**
 * iKuai OS V4 (LAN https://192.168.1.1:1443) 端口映射(dnat) 只读/写入工具
 *
 * 用法:
 *   node scripts/ikuai-dnat.mjs plan                     # 只打印将提交的报文，不联网
 *   IKUAI_USER=admin IKUAI_PASS='***' node scripts/ikuai-dnat.mjs show
 *   IKUAI_USER=admin IKUAI_PASS='***' node scripts/ikuai-dnat.mjs add --dry-run
 *   IKUAI_USER=admin IKUAI_PASS='***' node scripts/ikuai-dnat.mjs add
 *   IKUAI_USER=admin IKUAI_PASS='***' node scripts/ikuai-dnat.mjs del --ids 3,4,5
 *
 * 说明:
 *   - 登录体格式来自前端 bundle (static/js/index-*.js):
 *       { username, passwd: md5hex(password), pass: base64("salt_11" + password) }
 *     成功后服务端下发 cookie sess_key。
 *   - 所有调用都是 POST {base}/Action/call, body = { func_name, action, param }。
 *   - 失败不吞异常: 非 code=0 / HTTP!=200 直接抛错并以非 0 退出。
 */
import crypto from "node:crypto";

const BASE = process.env.IKUAI_BASE || "https://192.168.1.1:1443";
const USER = process.env.IKUAI_USER || "";
const PASS = process.env.IKUAI_PASS || "";
const TARGET_IP = process.env.IKUAI_TARGET_IP || "192.168.1.15";

process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0"; // 路由器自签证书

const md5hex = (s) => crypto.createHash("md5").update(s, "utf8").digest("hex");

function loginBody(user, pass) {
  return { username: user, passwd: md5hex(pass), pass: Buffer.from("salt_11" + pass, "utf8").toString("base64") };
}

/** 端口映射三条规则的提交体（字段与枚举取自前端 addOrEdit-*.js） */
function rules() {
  return [
    { tagname: "fm-edge-15080", enabled: "yes", lan_addr: TARGET_IP, lan_port: "80",
      protocol: "tcp", interface: "all", wan_port: "15080",
      src_addr: { custom: [], object: [] }, comment: "full-mall-edge-http" },
    { tagname: "fm-edge-15443", enabled: "yes", lan_addr: TARGET_IP, lan_port: "443",
      protocol: "tcp", interface: "all", wan_port: "15443",
      src_addr: { custom: [], object: [] }, comment: "full-mall-edge-https" },
    { tagname: "fm-shop-15051", enabled: "yes", lan_addr: TARGET_IP, lan_port: "41285",
      protocol: "tcp", interface: "all", wan_port: "15051",
      src_addr: { custom: [], object: [] }, comment: "full-mall-storefront-41285" },
    { tagname: "fm-admin-15052", enabled: "yes", lan_addr: TARGET_IP, lan_port: "41281",
      protocol: "tcp", interface: "all", wan_port: "15052",
      src_addr: { custom: [], object: [] }, comment: "full-mall-admin-41281" }
  ];
}

async function login() {
  if (!USER || !PASS) throw new Error("缺少 IKUAI_USER / IKUAI_PASS 环境变量");
  const res = await fetch(BASE + "/Action/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(loginBody(USER, PASS)),
  });
  const text = await res.text();
  const json = JSON.parse(text);
  if (json.code !== 0) {
    // 1005=账号或密码错误, 1006=锁定5分钟, 1008=会话过期
    throw new Error("登录失败: HTTP " + res.status + " " + text);
  }
  const m = /sess_key=([^;]+)/.exec(res.headers.get("set-cookie") || "");
  if (!m) throw new Error("登录返回 code=0 但没有 sess_key cookie: " + text);
  return m[1];
}

async function call(key, func_name, action, param) {
  const res = await fetch(BASE + "/Action/call", {
    method: "POST",
    headers: { "Content-Type": "application/json", Cookie: "sess_key=" + key },
    body: JSON.stringify({ func_name, action, param }),
  });
  const text = await res.text();
  const json = JSON.parse(text);
  if (json.code !== 0) throw new Error(`${func_name}/${action} 失败: ` + text);
  return json;
}

function plan() {
  return rules().map((r) => ({ func_name: "dnat", action: "add", param: r }));
}

async function main() {
  const [cmd, ...rest] = process.argv.slice(2);
  const dry = rest.includes("--dry-run");
  const idsArg = (() => { const i = rest.indexOf("--ids"); return i >= 0 ? rest[i + 1] : ""; })();

  if (cmd === "plan") {
    console.log(JSON.stringify(plan(), null, 2));
    return;
  }
  if (cmd === "show") {
    const key = await login();
    const json = await call(key, "dnat", "show", {
      TYPE: "data,total,enabled_total,disabled_total", limit: "0,200",
    });
    console.log(JSON.stringify(json, null, 2));
    return;
  }
  if (cmd === "add") {
    if (dry) { console.log(JSON.stringify(plan(), null, 2)); return; }
    const key = await login();
    const before = await call(key, "dnat", "show", { TYPE: "data,total,enabled_total,disabled_total", limit: "0,200" });
    const used = new Set((before.results?.data ?? []).map((item) => String(item.wan_port)));
    for (const item of plan()) {
      if (used.has(String(item.param.wan_port))) {
        console.log("SKIP " + item.param.wan_port + "（已存在）");
        continue;
      }
      const r = await call(key, item.func_name, item.action, item.param);
      console.log("ADDED " + item.param.tagname + " -> " + JSON.stringify(r));
    }
    const after = await call(key, "dnat", "show", { TYPE: "data,total,enabled_total,disabled_total", limit: "0,200" });
    console.log("AFTER " + JSON.stringify(after));
    return;
  }
  if (cmd === "del") {
    if (!idsArg) throw new Error("del 需要 --ids，例如 --ids 3,4,5 （多个用逗号，规则来自 show 的 id）");
    if (dry) { console.log(JSON.stringify({ func_name: "dnat", action: "del", param: { id: idsArg } }, null, 2)); return; }
    const key = await login();
    const r = await call(key, "dnat", "del", { id: idsArg });
    console.log("DELETED " + idsArg + " -> " + JSON.stringify(r));
    return;
  }
  throw new Error("未知子命令: " + (cmd || "(空)") + "；可用: plan | show | add | del");
}

main().catch((e) => { console.error("[ERROR] " + e.message); process.exit(1); });
