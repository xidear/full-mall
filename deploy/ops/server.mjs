import http from 'node:http';
import fs from 'node:fs';
import fsp from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawn } from 'node:child_process';

const PORT = Number(process.env.PORT || 41287);
const DOCKER_SOCK = process.env.DOCKER_SOCK || '/var/run/docker.sock';
const OPS_TOKEN = (process.env.OPS_TOKEN || '').trim();
const HOST_ETC = process.env.HOST_ETC || '/host/etc';
const MANAGE_PREFIX = process.env.OPS_MANAGE_PREFIX || 'fullmall-';
const VERSION = '1.0.0';
const STARTED_AT = Date.now();

const SSL_DOMAIN = (process.env.SSL_DOMAIN || '').trim().toLowerCase();
const SSL_EMAIL = (process.env.SSL_EMAIL || '').trim();
const SSL_CA = (process.env.SSL_CA || 'letsencrypt').trim();
const SSL_AUTO_ISSUE = (process.env.SSL_AUTO_ISSUE || 'true') === 'true';
const SSL_DAYS_BEFORE = Number(process.env.SSL_DAYS_BEFORE || 30);
const CERT_DIR = process.env.SSL_CERT_DIR || '/certs';
const WEBROOT = process.env.SSL_WEBROOT || '/var/www/acme';
const ACME_HOME = process.env.ACME_HOME || '/acme-data';
const ACME_SH = process.env.ACME_SH || '/opt/acme.sh/acme.sh';
const SSL_WAIT_SECONDS = Number(process.env.SSL_WAIT_SECONDS || 45);
const SSL_CHALLENGE = (process.env.SSL_CHALLENGE || 'http').trim().toLowerCase();
const DP_ID = (process.env.DP_ID || '').trim();
const DP_KEY = (process.env.DP_KEY || '').trim();

const sslState = { lastRunAt: null, lastError: null, lastOutput: '', running: false };

function log(...args) { console.log(new Date().toISOString(), ...args); }

function iso(seconds) {
  if (!seconds || !Number.isFinite(Number(seconds))) return null;
  return new Date(Number(seconds) * 1000).toISOString();
}

function fail(message, code = 500) {
  const error = new Error(message);
  error.statusCode = code;
  return error;
}

function authOk(req) {
  if (!OPS_TOKEN) return true;
  const header = req.headers.authorization || '';
  if (!header.startsWith('Bearer ')) return false;
  const supplied = Buffer.from(header.slice(7));
  const expected = Buffer.from(OPS_TOKEN);
  return supplied.length === expected.length && crypto.timingSafeEqual(supplied, expected);
}

async function readJsonBody(req) {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  if (!chunks.length) return {};
  const text = Buffer.concat(chunks).toString('utf8');
  try { return JSON.parse(text); } catch (error) { throw fail('请求体不是合法 JSON: ' + error.message, 400); }
}

function dockerRequest(apiPath, method = 'GET') {
  return new Promise((resolve, reject) => {
    const request = http.request({ socketPath: DOCKER_SOCK, path: apiPath, method, headers: { Host: 'docker', 'Content-Length': 0 } }, (response) => {
      const chunks = [];
      response.on('data', (chunk) => chunks.push(chunk));
      response.on('end', () => {
        const body = Buffer.concat(chunks).toString('utf8');
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(fail('Docker API ' + apiPath + ' 返回 HTTP ' + response.statusCode + ': ' + body.slice(0, 500), 502));
          return;
        }
        if (!body) { resolve({}); return; }
        try { resolve(JSON.parse(body)); } catch (error) { reject(fail('Docker API ' + apiPath + ' 返回非 JSON: ' + error.message, 502)); }
      });
    });
    request.setTimeout(15000, () => request.destroy(fail('Docker API ' + apiPath + ' 超时', 504)));
    request.on('error', (error) => reject(fail('Docker API ' + apiPath + ' 失败: ' + error.message, 502)));
    request.end();
  });
}

async function readText(file) {
  try { return await fsp.readFile(file, 'utf8'); } catch (error) { if (error.code === 'ENOENT') return null; throw error; }
}

function parseOsRelease(text) {
  const result = {};
  if (!text) return result;
  for (const line of text.split('\n')) {
    const match = line.match(/^([A-Z0-9_]+)=(.*)$/);
    if (!match) continue;
    result[match[1]] = match[2].replace(/^"|"$/g, '');
  }
  return result;
}

function diskUsage() {
  const stat = fs.statfsSync('/');
  const total = stat.blocks * stat.bsize;
  const free = stat.bfree * stat.bsize;
  const available = stat.bavail * stat.bsize;
  const used = total - free;
  return { totalBytes: total, usedBytes: used, freeBytes: free, availableBytes: available,
    usedPercent: total > 0 ? Number(((used / total) * 100).toFixed(1)) : 0 };
}

function hostInfo() {
  const cpus = os.cpus();
  const totalMem = os.totalmem();
  const freeMem = os.freemem();
  const ips = [];
  for (const [name, addresses] of Object.entries(os.networkInterfaces())) {
    for (const address of addresses || []) {
      if (address.family === 'IPv4' && !address.internal) ips.push({ interface: name, address: address.address, mac: address.mac });
    }
  }
  return {
    hostname: os.hostname(), platform: os.platform(), release: os.release(), arch: os.arch(),
    uptimeSeconds: Math.round(os.uptime()), load: os.loadavg().map((value) => Number(value.toFixed(2))),
    cpu: { cores: cpus.length, model: cpus[0]?.model || 'unknown', speedMhz: cpus[0]?.speed || null },
    memory: { totalBytes: totalMem, freeBytes: freeMem, usedBytes: totalMem - freeMem,
      usedPercent: totalMem > 0 ? Number((((totalMem - freeMem) / totalMem) * 100).toFixed(1)) : 0 },
    disk: diskUsage(), ips, nodeVersion: process.version, agentVersion: VERSION,
    agentStartedAt: new Date(STARTED_AT).toISOString(), timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
  };
}

function containerName(container) { return (container.Names?.[0] || container.Id?.slice(0, 12) || '').replace(/^\//, ''); }

function containerManaged(container) {
  return containerName(container).startsWith(MANAGE_PREFIX) || container.Labels?.['com.fullmall.ops.manage'] === 'true';
}

function securityWarnings(container, inspect) {
  const warnings = [];
  const hostConfig = inspect?.HostConfig || {};
  const config = inspect?.Config || {};
  if (hostConfig.Privileged) warnings.push('privileged 模式运行，拥有宿主全部能力');
  if (hostConfig.NetworkMode === 'host') warnings.push('使用 host 网络，端口与宿主共享');
  for (const mount of inspect?.Mounts || []) {
    if (mount.Source === '/var/run/docker.sock' || mount.Destination === '/var/run/docker.sock') warnings.push('挂载了 Docker socket，容器内可控制宿主 Docker');
  }
  if ((container.Image || '').endsWith(':latest')) warnings.push('镜像使用 latest 标签，版本不可复现');
  if (!config.Healthcheck && container.State === 'running') warnings.push('未配置 healthcheck，无法判断服务是否真的可用');
  if (!config.User) warnings.push('以 root 用户运行');
  return warnings;
}

function cpuPercent(stats) {
  const cpu = stats?.cpu_stats;
  const pre = stats?.precpu_stats;
  if (!cpu || !pre) return null;
  const cpuDelta = (cpu.cpu_usage?.total_usage || 0) - (pre.cpu_usage?.total_usage || 0);
  const systemDelta = (cpu.system_cpu_usage || 0) - (pre.system_cpu_usage || 0);
  const online = cpu.online_cpus || cpu.cpu_usage?.percpu_usage?.length || os.cpus().length;
  if (systemDelta <= 0 || cpuDelta < 0) return null;
  return Number(((cpuDelta / systemDelta) * online * 100).toFixed(2));
}

function memoryStats(stats) {
  const usage = stats?.memory_stats?.usage;
  const limit = stats?.memory_stats?.limit;
  if (!Number.isFinite(usage) || !Number.isFinite(limit) || limit <= 0) return null;
  const cache = stats.memory_stats.stats?.inactive_file ?? stats.memory_stats.stats?.cache ?? 0;
  const used = Math.max(0, usage - cache);
  return { usageBytes: used, limitBytes: limit, percent: Number(((used / limit) * 100).toFixed(1)) };
}

async function mapLimit(items, limit, mapper) {
  const result = new Array(items.length);
  let cursor = 0;
  async function worker() { while (cursor < items.length) { const index = cursor++; result[index] = await mapper(items[index], index); } }
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, worker));
  return result;
}

async function containerDetails(container) {
  const id = container.Id;
  const state = container.State || 'unknown';
  let inspect = null;
  let stats = null;
  try { inspect = await dockerRequest('/containers/' + id + '/json'); } catch (error) { log('inspect container failed', containerName(container), error.message); }
  if (state === 'running') {
    try { stats = await dockerRequest('/containers/' + id + '/stats?stream=false'); } catch (error) { log('stats container failed', containerName(container), error.message); }
  }
  return {
    id: id.slice(0, 12), fullId: id, name: containerName(container), image: container.Image, state,
    status: container.Status, created: iso(container.Created) || container.Created,
    uptimeSeconds: state === 'running' && container.Created ? Math.round(Date.now() / 1000 - container.Created) : 0,
    ports: (container.Ports || []).map((port) => ({ privatePort: port.PrivatePort, publicPort: port.PublicPort || null, type: port.Type, ip: port.IP || null })),
    labels: container.Labels || {}, managed: containerManaged(container),
    restartPolicy: inspect?.HostConfig?.RestartPolicy?.Name || 'unknown',
    networkMode: inspect?.HostConfig?.NetworkMode || null,
    cpuPercent: stats ? cpuPercent(stats) : null,
    memory: stats ? memoryStats(stats) : null,
    securityWarnings: securityWarnings(container, inspect)
  };
}

function parseSshd(text) {
  const result = { PermitRootLogin: null, PasswordAuthentication: null, Port: null };
  if (!text) return result;
  for (const raw of text.split('\n')) {
    const line = raw.trim();
    if (!line || line.startsWith('#')) continue;
    const [key, ...rest] = line.split(/\s+/);
    if (key in result) result[key] = rest.join(' ');
  }
  return result;
}

async function securityChecks(containers) {
  const checks = [];
  const add = (id, severity, title, status, evidence, remediation) => checks.push({ id, severity, title, status, evidence, remediation });
  let socketMode = null;
  try { socketMode = (fs.statSync(DOCKER_SOCK).mode & 0o777).toString(8); }
  catch (error) { add('docker-socket', 'high', 'Docker socket 可访问', 'fail', DOCKER_SOCK + ' 不可读: ' + error.message, '确认 /var/run/docker.sock 已挂载，且 agent 有权限访问'); }
  if (socketMode) add('docker-socket', 'high', 'Docker socket 权限', socketMode.endsWith('6') || socketMode.endsWith('7') ? 'warn' : 'pass', DOCKER_SOCK + ' mode=' + socketMode, '保持 660 root:docker；不要 chmod 666');
  const sshd = parseSshd(await readText(path.join(HOST_ETC, 'ssh/sshd_config')));
  if (sshd.PermitRootLogin === null && sshd.PasswordAuthentication === null) {
    add('ssh-config', 'medium', 'SSH 配置可审计', 'unknown', '未找到 ' + path.join(HOST_ETC, 'ssh/sshd_config') + '（容器需挂载 /etc:/host/etc:ro）', '在 compose 中挂载 /etc:/host/etc:ro，或手工执行 sshd -T 检查');
  } else {
    const rootLogin = (sshd.PermitRootLogin || '').toLowerCase();
    add('ssh-root-login', 'high', 'SSH 禁止 root 直接登录', rootLogin === 'yes' ? 'warn' : 'pass', 'PermitRootLogin=' + (sshd.PermitRootLogin || '默认'), '建议 PermitRootLogin prohibit-password，并改用普通用户 + sudo');
    const passwordAuth = (sshd.PasswordAuthentication || '').toLowerCase();
    add('ssh-password-auth', 'medium', 'SSH 密码登录', passwordAuth === 'yes' ? 'warn' : 'pass', 'PasswordAuthentication=' + (sshd.PasswordAuthentication || '默认'), '生产建议关闭密码登录，使用 SSH key；至少配合 fail2ban');
  }
  const ufw = await readText(path.join(HOST_ETC, 'ufw/ufw.conf'));
  const firewalld = await readText(path.join(HOST_ETC, 'firewalld/firewalld.conf'));
  const nftables = await readText(path.join(HOST_ETC, 'nftables.conf'));
  const firewallEnabled = /ENABLED=yes/i.test(ufw || '') || /FirewallBackend/i.test(firewalld || '') || Boolean(nftables);
  add('firewall', 'high', '主机防火墙', firewallEnabled ? 'pass' : 'unknown', firewallEnabled ? '检测到 ufw/firewalld/nftables 配置' : '未检测到 ufw/firewalld/nftables 配置', '只放行 22/80/443 与业务端口；数据库/Redis 不要暴露公网');
  const fail2ban = await readText(path.join(HOST_ETC, 'fail2ban/jail.local')) || await readText(path.join(HOST_ETC, 'fail2ban/jail.conf'));
  add('fail2ban', 'medium', 'SSH 暴力破解防护', fail2ban ? 'pass' : 'warn', fail2ban ? '检测到 fail2ban 配置' : '未检测到 fail2ban', '安装 fail2ban 并启用 sshd jail；或使用云防火墙');
  const disk = diskUsage();
  add('disk', 'high', '磁盘空间', disk.usedPercent >= 95 ? 'fail' : disk.usedPercent >= 85 ? 'warn' : 'pass', '根分区已用 ' + disk.usedPercent + '%', '清理日志/镜像：docker system prune -af；日志轮转见 deploy 文档');
  const mem = hostInfo().memory;
  add('memory', 'medium', '内存压力', mem.usedPercent >= 90 ? 'warn' : 'pass', '内存已用 ' + mem.usedPercent + '%', '检查 docker stats；为容器设置 mem_limit，避免 OOM 拖垮宿主');
  const latest = containers.filter((item) => (item.image || '').endsWith(':latest'));
  add('latest-tag', 'medium', '镜像版本可复现', latest.length ? 'warn' : 'pass', latest.length ? latest.map((item) => item.name).join(', ') + ' 使用 latest' : '未发现 latest 标签', '生产固定镜像 tag/digest，升级走发布流程');
  const privileged = containers.filter((item) => item.securityWarnings.some((w) => w.includes('privileged')));
  add('privileged', 'high', '特权容器', privileged.length ? 'warn' : 'pass', privileged.length ? privileged.map((item) => item.name).join(', ') : '无 privileged 容器', '去掉 --privileged，按需添加 cap_add');
  const hostNetwork = containers.filter((item) => item.networkMode === 'host');
  add('host-network', 'medium', 'host 网络容器', hostNetwork.length ? 'warn' : 'pass', hostNetwork.length ? hostNetwork.map((item) => item.name).join(', ') : '无 host 网络容器', '除边缘代理/agent 外，业务容器使用 bridge 网络并显式发布端口');
  const sockMount = containers.filter((item) => item.securityWarnings.some((w) => w.includes('Docker socket')));
  add('docker-sock-mount', 'high', 'Docker socket 暴露', sockMount.length ? 'warn' : 'pass', sockMount.length ? sockMount.map((item) => item.name).join(', ') : '无容器挂载 docker.sock', '仅运维 agent 挂载只读 socket；业务容器不要挂载');
  const noHealth = containers.filter((item) => item.state === 'running' && item.securityWarnings.some((w) => w.includes('healthcheck')));
  add('healthcheck', 'low', '容器健康检查', noHealth.length ? 'warn' : 'pass', noHealth.length ? noHealth.map((item) => item.name).join(', ') : '运行中容器均有 healthcheck', '为关键容器增加 healthcheck，并在 compose 中使用 depends_on.condition');
  const noRestart = containers.filter((item) => item.managed && item.restartPolicy === 'no');
  add('restart-policy', 'medium', '业务容器自动重启', noRestart.length ? 'warn' : 'pass', noRestart.length ? noRestart.map((item) => item.name).join(', ') : '业务容器重启策略正常', '设置 restart: unless-stopped；数据库等有状态服务谨慎使用 always');
  const penalty = checks.reduce((sum, check) => sum + (check.status === 'fail' ? 15 : check.status === 'warn' ? 5 : check.status === 'unknown' ? 1 : 0), 0);
  const findings = checks.filter((check) => check.status !== 'pass');
  return { score: Math.max(0, 100 - penalty), checks, findings,
    summary: { pass: checks.filter((c) => c.status === 'pass').length, warn: checks.filter((c) => c.status === 'warn').length,
      fail: checks.filter((c) => c.status === 'fail').length, unknown: checks.filter((c) => c.status === 'unknown').length } };
}

function run(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { env: { ...process.env, HOME: ACME_HOME, ...(options.env || {}) }, stdio: ['ignore', 'pipe', 'pipe'] });
    let stdout = '';
    let stderr = '';
    const timer = setTimeout(() => { child.kill('SIGKILL'); reject(fail(command + ' 超时（' + (options.timeoutMs || 300000) + 'ms）', 504)); }, options.timeoutMs || 300000);
    child.stdout.on('data', (chunk) => { stdout += chunk.toString(); });
    child.stderr.on('data', (chunk) => { stderr += chunk.toString(); });
    child.on('error', (error) => { clearTimeout(timer); reject(error); });
    child.on('close', (code) => { clearTimeout(timer); resolve({ code, stdout, stderr }); });
  });
}

function certPaths(domain) {
  const dir = path.join(CERT_DIR, domain);
  return { dir, fullchain: path.join(dir, 'fullchain.pem'), key: path.join(dir, 'privkey.pem'), cert: path.join(dir, 'cert.pem'), chain: path.join(dir, 'chain.pem') };
}

async function readCertificate(domain) {
  const paths = certPaths(domain);
  let text;
  try { text = await fsp.readFile(paths.fullchain, 'utf8'); } catch (error) { if (error.code === 'ENOENT') return null; throw error; }
  const result = await run('openssl', ['x509', '-noout', '-in', paths.fullchain, '-subject', '-issuer', '-dates', '-serial', '-fingerprint', '-sha256'], { timeoutMs: 15000 });
  if (result.code !== 0) throw fail('openssl 读取证书失败: ' + (result.stderr || result.stdout), 500);
  const parsed = {};
  for (const line of result.stdout.split('\n')) { const index = line.indexOf('='); if (index >= 0) parsed[line.slice(0, index).trim()] = line.slice(index + 1).trim(); }
  const notAfter = new Date(parsed.notAfter);
  const notBefore = new Date(parsed.notBefore);
  if (Number.isNaN(notAfter.getTime()) || Number.isNaN(notBefore.getTime())) throw fail('证书日期无法解析: ' + result.stdout, 500);
  const daysLeft = Math.floor((notAfter.getTime() - Date.now()) / 86400000);
  const issuer = parsed.issuer || '';
  let source = issuer;
  if (/Let's Encrypt/i.test(issuer)) source = "Let's Encrypt";
  else if (/ZeroSSL/i.test(issuer)) source = 'ZeroSSL';
  else if (/Buypass/i.test(issuer)) source = 'Buypass';
  else if (/Google Trust Services/i.test(issuer)) source = 'Google Trust Services';
  else if (issuer && issuer === (parsed.subject || '')) source = '自签名（占位）';
  let lastRenewAt = null;
  try { lastRenewAt = (await fsp.stat(paths.fullchain)).mtime.toISOString(); } catch (error) { if (error.code !== 'ENOENT') throw error; }
  return { domain, source, issuer, subject: parsed.subject || '', serial: parsed.serial || '',
    fingerprint: parsed['sha256 Fingerprint'] || '', validFrom: notBefore.toISOString(), validTo: notAfter.toISOString(),
    daysLeft, status: daysLeft < 0 ? 'expired' : daysLeft <= SSL_DAYS_BEFORE ? 'expiring' : 'valid', lastRenewAt,
    paths: { fullchain: paths.fullchain, key: paths.key } };
}

async function sslStatus() {
  if (!SSL_DOMAIN) return { ok: true, enabled: false, certificates: [], message: '本机未启用 SSL（SSL_DOMAIN 为空，通常只有边缘节点启用）', checkedAt: new Date().toISOString() };
  const certificate = await readCertificate(SSL_DOMAIN);
  return { ok: true, enabled: true, domain: SSL_DOMAIN, ca: SSL_CA, autoRenew: SSL_AUTO_ISSUE,
    renewalWindowDays: SSL_DAYS_BEFORE, certificates: certificate ? [certificate] : [], missing: !certificate,
    state: sslState, checkedAt: new Date().toISOString() };
}

async function ensureDirs() {
  await fsp.mkdir(CERT_DIR, { recursive: true });
  await fsp.mkdir(WEBROOT, { recursive: true });
  await fsp.mkdir(ACME_HOME, { recursive: true });
  await fsp.mkdir(path.join(CERT_DIR, SSL_DOMAIN || '_disabled'), { recursive: true });
}

function acmeBaseArgs() { return ['--home', ACME_HOME, '--config-home', ACME_HOME]; }

async function installCert(domain) {
  const paths = certPaths(domain);
  const args = [...acmeBaseArgs(), '--install-cert', '-d', domain, '--key-file', paths.key,
    '--fullchain-file', paths.fullchain, '--cert-file', paths.cert, '--ca-file', paths.chain,
    '--reloadcmd', 'touch ' + path.join(CERT_DIR, '.reload')];
  const result = await run('bash', [ACME_SH, ...args], { timeoutMs: 120000 });
  if (result.code !== 0) throw fail('安装证书失败: ' + (result.stderr || result.stdout), 500);
  return result;
}

function challengeArgs() {
  if (SSL_CHALLENGE === 'dns') {
    if (!DP_ID || !DP_KEY) {
      throw fail('SSL_CHALLENGE=dns 但缺少 DP_ID / DP_KEY（DNSPod API Token）', 500);
    }
    return ['--dns', 'dns_dp'];
  }
  return ['--webroot', WEBROOT];
}

function challengeEnv() {
  return SSL_CHALLENGE === 'dns' ? { DP_Id: DP_ID, DP_Key: DP_KEY } : {};
}

async function issueCertificate(domain, force = false) {
  const args = [...acmeBaseArgs(), '--issue', ...challengeArgs(), '-d', domain, '--server', SSL_CA, '--keylength', '2048'];
  if (SSL_EMAIL) args.push('--accountemail', SSL_EMAIL);
  if (force) args.push('--force');
  const result = await run('bash', [ACME_SH, ...args], { timeoutMs: 300000, env: challengeEnv() });
  if (result.code !== 0) throw fail('申请证书失败（' + domain + '）: ' + (result.stdout + '\n' + result.stderr).trim(), 502);
  await installCert(domain);
  return result.stdout + '\n' + result.stderr;
}

async function renewCertificate(domain, force = true) {
  const paths = certPaths(domain);
  if (!fs.existsSync(paths.fullchain)) return issueCertificate(domain, force);
  const args = [...acmeBaseArgs(), '--renew', '-d', domain];
  if (force) args.push('--force');
  if (SSL_EMAIL) args.push('--accountemail', SSL_EMAIL);
  const result = await run('bash', [ACME_SH, ...args], { timeoutMs: 300000, env: challengeEnv() });
  if (result.code !== 0) throw fail('续签失败（' + domain + '）: ' + (result.stderr || result.stdout), 502);
  await installCert(domain);
  return result.stdout + '\n' + result.stderr;
}

let sslLock = Promise.resolve();
function withSslLock(task) { const next = sslLock.then(task, task); sslLock = next.catch(() => {}); return next; }

async function refreshSsl(reason, force) {
  if (!SSL_DOMAIN) return null;
  return withSslLock(async () => {
    sslState.running = true;
    sslState.lastRunAt = new Date().toISOString();
    sslState.lastError = null;
    try {
      const before = await readCertificate(SSL_DOMAIN);
      const need = !before || before.daysLeft <= SSL_DAYS_BEFORE;
      let output = '';
      if (need || force) {
        log('ssl ' + (before ? 'renew' : 'issue') + ' start', SSL_DOMAIN, 'reason=' + reason);
        const selfSigned = before?.source?.startsWith('自签名') === true;
        output = before && !selfSigned
          ? await renewCertificate(SSL_DOMAIN, force)
          : await issueCertificate(SSL_DOMAIN, force);
      } else {
        output = '证书仍在有效期内（' + before.daysLeft + ' 天），跳过续签';
      }
      sslState.lastOutput = output;
      const after = await readCertificate(SSL_DOMAIN);
      log('ssl done', SSL_DOMAIN, after?.daysLeft + ' days');
      return { certificate: after, output };
    } catch (error) {
      sslState.lastError = error.message;
      log('ssl failed', error.message);
      throw error;
    } finally { sslState.running = false; }
  });
}

async function waitForPort80() {
  const deadline = Date.now() + SSL_WAIT_SECONDS * 1000;
  while (Date.now() < deadline) {
    try {
      const response = await fetch('http://127.0.0.1/.well-known/acme-challenge/health', { signal: AbortSignal.timeout(2000) });
      if (response.status < 500) return true;
    } catch { /* 边缘容器可能还没起来：继续等，超时后由上层抛出明确错误 */ }
    await new Promise((resolve) => setTimeout(resolve, 3000));
  }
  return false;
}

async function bootstrapSsl() {
  if (!SSL_DOMAIN || !SSL_AUTO_ISSUE) return;
  try {
    const current = await readCertificate(SSL_DOMAIN);
    if (current && current.daysLeft > SSL_DAYS_BEFORE) { log('ssl certificate valid', current.daysLeft, 'days'); return; }
    if (SSL_CHALLENGE !== 'dns') {
      const ready = await waitForPort80();
      if (!ready) throw fail('等待 80 端口超时（' + SSL_WAIT_SECONDS + 's），请确认路由器 80→本机 80 已映射');
    }
    await refreshSsl('bootstrap', true);
  } catch (error) { sslState.lastError = error.message; log('ssl bootstrap failed:', error.message); }
}

async function handle(req) {
  const url = new URL(req.url, 'http://agent');
  const pathname = url.pathname;
  if (!authOk(req)) throw fail('未授权：Bearer token 不匹配', 401);
  if (req.method === 'GET' && pathname === '/health') return { ok: true, version: VERSION, uptimeSeconds: Math.round((Date.now() - STARTED_AT) / 1000) };
  if (req.method === 'GET' && pathname === '/api/ssl') return sslStatus();
  if (req.method === 'POST' && pathname === '/api/ssl/renew') {
    const body = await readJsonBody(req);
    const domain = (body.domain || SSL_DOMAIN || '').trim().toLowerCase();
    if (!domain) throw fail('未配置 SSL_DOMAIN', 400);
    const result = await refreshSsl('manual', true);
    return { ok: true, domain, certificate: result?.certificate || null, output: result?.output || '' };
  }
  if (req.method === 'POST' && pathname === '/api/ssl/issue') {
    const body = await readJsonBody(req);
    const domain = (body.domain || SSL_DOMAIN || '').trim().toLowerCase();
    if (!domain) throw fail('未配置 SSL_DOMAIN', 400);
    return withSslLock(async () => {
      sslState.running = true; sslState.lastRunAt = new Date().toISOString();
      try { const output = await issueCertificate(domain, true); const certificate = await readCertificate(domain); return { ok: true, domain, certificate, output }; }
      finally { sslState.running = false; }
    });
  }
  if (req.method === 'GET' && pathname === '/api/ops') {
    const [version, info, list] = await Promise.all([dockerRequest('/version'), dockerRequest('/info'), dockerRequest('/containers/json?all=1')]);
    const containers = await mapLimit(list, 4, containerDetails);
    const security = await securityChecks(containers);
    return { ok: true, generatedAt: new Date().toISOString(), host: hostInfo(),
      docker: { version: version.Version, apiVersion: version.ApiVersion, os: version.Os, arch: version.Arch,
        containers: info.Containers, containersRunning: info.ContainersRunning, containersPaused: info.ContainersPaused,
        containersStopped: info.ContainersStopped, images: info.Images, driver: info.Driver, rootDir: info.DockerRootDir, serverVersion: info.ServerVersion },
      containers, security, ssl: await sslStatus() };
  }
  const actionMatch = pathname.match(/^\/api\/containers\/([^/]+)\/(start|stop|restart)$/);
  if (req.method === 'POST' && actionMatch) {
    const id = decodeURIComponent(actionMatch[1]);
    const action = actionMatch[2];
    const inspect = await dockerRequest('/containers/' + encodeURIComponent(id) + '/json');
    const summary = { Id: inspect.Id, Names: [inspect.Name], Labels: inspect.Config?.Labels || {}, Image: inspect.Config?.Image };
    if (!containerActionAllowed(summary, action)) throw fail('容器 ' + containerName(summary) + ' 不在运维白名单（前缀 ' + MANAGE_PREFIX + ' 或 label com.fullmall.ops.manage=true）', 403);
    await dockerRequest('/containers/' + encodeURIComponent(id) + '/' + action + (action === 'stop' ? '?t=10' : ''), 'POST');
    log('container action', containerName(summary), action);
    return { ok: true, id: inspect.Id.slice(0, 12), name: containerName(summary), action };
  }
  throw fail('未知路径: ' + req.method + ' ' + pathname, 404);
}

function containerActionAllowed(container, action) {
  if (!['start', 'stop', 'restart'].includes(action)) return false;
  const name = containerName(container);
  return containerManaged(container) || name.startsWith(MANAGE_PREFIX);
}

const server = http.createServer(async (req, res) => {
  try {
    const result = await handle(req);
    const body = JSON.stringify(result);
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Content-Length': Buffer.byteLength(body) });
    res.end(body);
  } catch (error) {
    const status = error.statusCode || 500;
    if (status >= 500) log('request failed', req.method, req.url, error.stack || error.message);
    const body = JSON.stringify({ ok: false, error: error.message, status });
    res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Content-Length': Buffer.byteLength(body) });
    res.end(body);
  }
});

await ensureDirs();
server.listen(PORT, '0.0.0.0', () => log('ops-agent listening on :' + PORT, 'ssl=' + (SSL_DOMAIN || 'disabled')));
if (SSL_DOMAIN && SSL_AUTO_ISSUE) {
  bootstrapSsl();
  setInterval(() => refreshSsl('scheduler', false).catch((error) => log('ssl scheduler failed:', error.message)), 6 * 3600 * 1000);
}
