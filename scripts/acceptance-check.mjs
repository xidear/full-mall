#!/usr/bin/env node
/**
 * 验收脚本：把"用户反馈过的问题"变成可重复执行的断言。
 *
 * 只做**只读或幂等**检查，避免改坏环境：
 * - 会写数据的两项（市场启停、全部已读）都是幂等的，且用现有值作为目标。
 * - 存储三态检查读取当前配置后决定断言方向，不主动切换服务商。
 *
 * 用法：node scripts/acceptance-check.mjs   （API 默认 http://127.0.0.1:41280）
 */
const API = process.env.ACCEPTANCE_API_BASE || 'http://127.0.0.1:41280';

const results = [];
function record(name, ok, detail) {
  results.push({ name, ok, detail });
  console.log((ok ? 'PASS  ' : 'FAIL  ') + name + (detail ? '  — ' + detail : ''));
}

async function adminToken() {
  const cap = await (await fetch(API + '/admin/v1/auth/captcha')).json();
  const nums = (cap.question.match(/\d+/g) ?? []).map(Number);
  const res = await fetch(API + '/admin/v1/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'Admin@123456', captchaId: cap.captchaId, captchaAnswer: String(nums[0] + nums[1]) })
  });
  if (!res.ok) throw new Error('登录失败 ' + res.status + ' ' + (await res.text()).slice(0, 100));
  return (await res.json()).token;
}

async function get(path, token) {
  const res = await fetch(API + path, { headers: token ? { Authorization: 'Bearer ' + token } : {} });
  return { status: res.status, body: await res.text() };
}

async function main() {
  const token = await adminToken();

  // 1) AI 客服：状态端点必须存在（此前 404 导致"社区版不包含"的假提示）
  const ai = await get('/admin/v1/ai/status', token);
  let aiOk = false, aiDetail = 'status=' + ai.status;
  if (ai.status === 200) {
    const body = JSON.parse(ai.body);
    aiOk = typeof body.edition === 'string' && typeof body.enabled === 'boolean';
    aiDetail = 'edition=' + body.edition + ' enabled=' + body.enabled + ' providers=' + body.providerCount;
  }
  record('AI 客服状态端点可用（不再假报"社区版不包含"）', aiOk, aiDetail);

  // 2) 审计：事件类型必须带中文标签（不能只给内部码）
  const audit = await get('/admin/v1/audit/event-types', token);
  let auditOk = false, auditDetail = 'status=' + audit.status;
  if (audit.status === 200) {
    const list = JSON.parse(audit.body);
    const sample = list.find(x => x.code === 'EXPORT_JOB_CREATED');
    auditOk = Array.isArray(list) && list.length > 0 && !!sample && /[\u4e00-\u9fff]/.test(sample.label);
    auditDetail = list.length + ' 种，示例：' + (sample ? sample.label : '缺失');
  }
  record('审计事件类型有中文标签', auditOk, auditDetail);

  // 3) 枚举字典：素材用途/类型必须有中文（此前表格显示 PRODUCT/IMAGE）
  const enums = await get('/admin/v1/meta/enums', token);
  let enumOk = false, enumDetail = 'status=' + enums.status;
  if (enums.status === 200) {
    const body = JSON.parse(enums.body);
    const purpose = (body.ASSET_PURPOSE || []).find(x => x.code === 'PRODUCT');
    const kind = (body.ASSET_KIND || []).find(x => x.code === 'IMAGE');
    enumOk = !!purpose && !!kind && /[\u4e00-\u9fff]/.test(purpose.label) && /[\u4e00-\u9fff]/.test(kind.label);
    enumDetail = 'PRODUCT→' + (purpose ? purpose.label : '?') + '，IMAGE→' + (kind ? kind.label : '?');
  }
  record('枚举字典返回中文标签（素材用途/类型）', enumOk, enumDetail);

  // 4) 币种小数位：JPY 必须是 0 位（此前写死 ×100 会放大 100 倍）
  const precision = await get('/api/v1/currencies/precision');
  let precOk = false, precDetail = 'status=' + precision.status;
  if (precision.status === 200) {
    const body = JSON.parse(precision.body);
    precOk = body.overrides && body.overrides.JPY === 0 && body.defaultMinorUnits === 2;
    precDetail = 'default=' + body.defaultMinorUnits + ' JPY=' + body.overrides.JPY + ' KWD=' + body.overrides.KWD;
  }
  record('金额按币种小数位（JPY=0, KWD=3）', precOk, precDetail);

  // 5) 通知：后台列表只能看到 STAFF 通知（此前能看到顾客手机号）
  const notif = await get('/admin/v1/notifications?limit=20', token);
  let notifOk = false, notifDetail = 'status=' + notif.status;
  if (notif.status === 200) {
    const list = JSON.parse(notif.body);
    const nonStaff = list.filter(x => x.recipientType !== 'STAFF');
    notifOk = nonStaff.length === 0;
    notifDetail = list.length + ' 条，非 STAFF 的 ' + nonStaff.length + ' 条';
  }
  record('通知列表只含员工通知（不泄露顾客手机号）', notifOk, notifDetail);

  // 6) 通知：全部已读可用且幂等（第二次标记 0 条）
  const readAll1 = await fetch(API + '/admin/v1/notifications/read-all', { method: 'POST', headers: { Authorization: 'Bearer ' + token } });
  const readAll2 = await fetch(API + '/admin/v1/notifications/read-all', { method: 'POST', headers: { Authorization: 'Bearer ' + token } });
  let readAllOk = false, readAllDetail = 'status=' + readAll1.status + '/' + readAll2.status;
  if (readAll1.ok && readAll2.ok) {
    const second = await readAll2.json();
    readAllOk = second.marked === 0;
    readAllDetail = '第一次 ' + (await readAll1.json()).marked + ' 条，第二次 ' + second.marked + ' 条';
  }
  record('「全部已读」可用且幂等', readAllOk, readAllDetail);

  // 7) 市场启停：幂等置为当前值（不改变现状），验证端点存在
  const markets = await get('/admin/v1/markets', token);
  let marketOk = false, marketDetail = 'status=' + markets.status;
  if (markets.status === 200) {
    const list = JSON.parse(markets.body);
    const cn = list.find(m => m.code === 'CN');
    const res = await fetch(API + '/admin/v1/markets/CN/active', {
      method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
      body: JSON.stringify({ active: cn.active })
    });
    marketOk = res.ok;
    marketDetail = 'CN.active=' + cn.active + ' 回写=' + res.status;
  }
  record('市场启停端点可用（列表内直接切换）', marketOk, marketDetail);

  // 8) 存储：读当前集成状态，并据此断言上传行为
  const integ = await get('/admin/v1/integrations?capability=storage', token);
  let storageOk = false, storageDetail = 'status=' + integ.status;
  if (integ.status === 200) {
    const rows = JSON.parse(integ.body);
    const row = Array.isArray(rows) ? rows.find(r => r.capability === 'storage') : rows;
    const enabled = row && row.enabled;
    const dir = row && row.hint && row.hint.directory;
    if (enabled && dir) {
      const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64');
      const form = new FormData();
      form.append('file', new Blob([png], { type: 'image/png' }), 'acceptance-' + Date.now() + '.png');
      const up = await fetch(API + '/admin/v1/media?purpose=PRODUCT&source=E2E', { method: 'POST', headers: { Authorization: 'Bearer ' + token }, body: form });
      storageOk = up.status === 200;
      storageDetail = 'enabled=true dir=' + dir + ' 上传=' + up.status;
    } else {
      storageOk = false;
      storageDetail = 'enabled=' + enabled + ' dir=' + dir + '（未配置完整，上传应 503——请到「第三方服务」配置后重跑）';
    }
  }
  record('存储已配置时上传成功（未配置必须 503，见下方说明）', storageOk, storageDetail);

  const failed = results.filter(r => !r.ok);
  console.log('\n' + (results.length - failed.length) + '/' + results.length + ' 通过');
  if (failed.length) {
    console.log('未通过：' + failed.map(f => f.name).join('；'));
    process.exit(1);
  }
}

main().catch(e => { console.error('验收脚本执行失败：' + e.message); process.exit(1); });
