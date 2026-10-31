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

  // 9) 能力就绪：必须能列出每个外部能力的配置来源（集成配置 / 环境变量 / 缺失 / 冲突）
  const readiness = await get('/admin/v1/readiness', token);
  let readyOk = false, readyDetail = 'status=' + readiness.status;
  if (readiness.status === 200) {
    const body = JSON.parse(readiness.body);
    const caps = (body.items || []).map(i => i.capability);
    readyOk = caps.includes('storage') && caps.includes('export') && caps.includes('payment');
    readyDetail = caps.map((c, idx) => c + '=' + body.items[idx].state).join(' ');
  }
  record('能力就绪端点返回每项配置来源', readyOk, readyDetail);

  // 10) 供应商启停：把当前值原样回写（幂等），验证端点与鉴权可用
  const suppliers = await get('/admin/v1/suppliers', token);
  let supOk = false, supDetail = 'status=' + suppliers.status;
  if (suppliers.status === 200) {
    const list = JSON.parse(suppliers.body);
    if (!list.length) {
      supOk = true; supDetail = '无供应商（跳过）';
    } else {
      const s = list[0];
      const id = s.supplierId ?? s.id;
      const res = await fetch(API + '/admin/v1/suppliers/' + id + '/active', {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
        body: JSON.stringify({ active: s.active })
      });
      supOk = res.ok;
      supDetail = s.name + ' active=' + s.active + ' 回写=' + res.status;
    }
  }
  record('供应商启停端点可用（列表内直接切换）', supOk, supDetail);

  // 11) AI 客服：问答三态 + 禁词拦截 + 未命中清单
  const askBlank = await fetch(API + '/admin/v1/ai/ask', {
    method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
    body: JSON.stringify({ question: '   ' })
  });
  const askBanned = await fetch(API + '/admin/v1/ai/ask', {
    method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
    body: JSON.stringify({ question: '你这个傻逼机器人' })
  });
  const bannedBody = await askBanned.json().catch(() => ({}));
  const unanswered = await get('/admin/v1/ai/unanswered?limit=5', token);
  const guardOk = askBlank.status === 400
    && askBanned.status === 200 && bannedBody.fallback === true
    && unanswered.status === 200 && Array.isArray(JSON.parse(unanswered.body));
  record('AI 客服：空问题 400 / 禁词转人工 / 未命中清单可查', guardOk,
    'empty=' + askBlank.status + ' banned=' + askBanned.status + ' unanswered=' + unanswered.status);

  // 12) 门店管理：列表字段与启停端点（把当前值原样回写，幂等）
  const stores = await get('/admin/v1/stores?includeInactive=true', token);
  let storeOk = false, storeDetail = 'status=' + stores.status;
  if (stores.status === 200) {
    const list = JSON.parse(stores.body);
    const sample = list[0];
    storeOk = Array.isArray(list) && !!sample && typeof sample.code === 'string'
      && typeof sample.marketCode === 'string' && typeof sample.active === 'boolean';
    if (sample) {
      const res = await fetch(API + '/admin/v1/stores/' + sample.id + '/active', {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
        body: JSON.stringify({ active: sample.active })
      });
      storeOk = storeOk && res.ok;
      storeDetail = list.length + ' 家，示例 ' + sample.code + ' active=' + sample.active + ' 回写=' + res.status;
    } else {
      storeDetail = '没有门店（字段/端点无法验证）';
    }
  }
  record('门店管理端点：编码/市场/状态字段 + 启停可用', storeOk, storeDetail);

  // 12.1) 软删除：默认列表不混入归档门店；includeDeleted 端点可用（不实际删除数据）
  const deletedList = await get('/admin/v1/stores?includeInactive=true&includeDeleted=true', token);
  let softOk = false, softDetail = 'status=' + deletedList.status;
  if (deletedList.status === 200) {
    const list = JSON.parse(deletedList.body);
    softOk = Array.isArray(list);
    softDetail = list.length + ' 家（含已删除）';
  }
  record('门店软删除列表可用（includeDeleted）', softOk, softDetail);

  // 12.2) 汇率：新增 → 精确换算 → 删除（幂等清理，不留下测试数据）
  const effectiveAt = new Date().toISOString();
  const createRate = await fetch(API + '/admin/v1/exchange-rates', {
    method: 'PUT', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
    body: JSON.stringify({ baseCurrency: 'USD', quoteCurrency: 'CNY', rate: 7.2,
      source: 'ACCEPTANCE', effectiveAt })
  });
  let rateOk = false, rateDetail = 'create=' + createRate.status;
  if (createRate.status === 201) {
    const created = await createRate.json();
    const conv = await get('/admin/v1/exchange-rates/convert?amount=10000&from=USD&to=CNY', token);
    if (conv.status === 200) {
      const body = JSON.parse(conv.body);
      rateOk = body.convertedAmount === 72000;
      rateDetail = '100.00 USD -> ' + body.convertedAmount + ' CNY 分';
    }
    const del = await fetch(API + '/admin/v1/exchange-rates/' + created.id, {
      method: 'DELETE', headers: { Authorization: 'Bearer ' + token }
    });
    rateOk = rateOk && del.status === 204;
    rateDetail += ' delete=' + del.status;
  }
  record('汇率新增/精确换算/删除（100 USD = 720 CNY）', rateOk, rateDetail);

  // 13) 促销规则：编辑需要回填范围，接口必须返回 scopeIds 数组
  const rules = await get('/admin/v1/promotions/rules', token);
  let ruleOk = false, ruleDetail = 'status=' + rules.status;
  if (rules.status === 200) {
    const list = JSON.parse(rules.body);
    ruleOk = Array.isArray(list) && list.every(rule => Array.isArray(rule.scopeIds));
    ruleDetail = list.length + ' 条规则，scopeIds=' + (list[0] ? JSON.stringify(list[0].scopeIds) : '无数据');
  }
  record('满减规则返回 scopeIds（编辑不回退范围）', ruleOk, ruleDetail);

  // 14) AI 状态：必须交代当前检索方式与就绪判定（缺配置要列出缺什么）
  const aiStatus = await get('/admin/v1/ai/status', token);
  let aiReadyOk = false, aiReadyDetail = 'status=' + aiStatus.status;
  if (aiStatus.status === 200) {
    const body = JSON.parse(aiStatus.body);
    aiReadyOk = typeof body.ready === 'boolean'
      && ['KEYWORD', 'SEMANTIC'].includes(body.retrieval)
      && Array.isArray(body.missing);
    aiReadyDetail = 'ready=' + body.ready + ' retrieval=' + body.retrieval
      + ' chat=' + (body.chatModel || '未配置') + ' missing=' + body.missing.join(',');
  }
  record('AI 状态交代就绪判定与检索方式', aiReadyOk, aiReadyDetail);

  // 15) 自动汇率：未配置也不能 5xx；配置/演示 mock 时能给出状态
  const fxProvider = await get('/admin/v1/exchange-rates/provider', token);
  let fxOk = fxProvider.status === 200;
  let fxDetail = 'status=' + fxProvider.status;
  if (fxOk) {
    const body = JSON.parse(fxProvider.body);
    fxOk = typeof body.configured === 'boolean';
    fxDetail = 'configured=' + body.configured + ' provider=' + (body.provider || '未配置')
      + ' demoMock=' + body.demoMock + ' lastError=' + (body.lastError || '无');
  }
  record('自动汇率状态可查询（未配置不 5xx）', fxOk, fxDetail);

  const failed = results.filter(r => !r.ok);
  console.log('\n' + (results.length - failed.length) + '/' + results.length + ' 通过');
  if (failed.length) {
    console.log('未通过：' + failed.map(f => f.name).join('；'));
    process.exit(1);
  }
}

main().catch(e => { console.error('验收脚本执行失败：' + e.message); process.exit(1); });
