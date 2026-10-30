<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'

interface HostInfo {
  hostname: string
  platform: string
  release: string
  arch: string
  uptimeSeconds: number
  load: number[]
  cpu: { cores: number; model: string; speedMhz: number | null }
  memory: { totalBytes: number; usedBytes: number; usedPercent: number }
  disk: { totalBytes: number; usedBytes: number; usedPercent: number }
  ips: { interface: string; address: string }[]
  nodeVersion: string
  agentVersion: string
  timezone: string
}

interface Container {
  id: string
  name: string
  image: string
  state: string
  status: string
  uptimeSeconds: number
  restartPolicy: string
  networkMode: string | null
  cpuPercent: number | null
  memory: { usageBytes: number; limitBytes: number; percent: number } | null
  ports: { privatePort: number; publicPort: number | null; type: string; ip: string | null }[]
  managed: boolean
  securityWarnings: string[]
}

interface SecurityCheck {
  id: string
  severity: string
  title: string
  status: string
  evidence: string
  remediation: string
}

interface Security {
  score: number
  checks: SecurityCheck[]
  findings: SecurityCheck[]
  summary: { pass: number; warn: number; fail: number; unknown: number }
}

interface Host {
  name: string
  url: string
  ok: boolean
  error?: string
  host?: HostInfo
  docker?: Record<string, unknown>
  containers?: Container[]
  security?: Security
}

interface Certificate {
  domain: string
  source: string
  issuer: string
  subject: string
  validFrom: string
  validTo: string
  daysLeft: number
  status: string
  lastRenewAt: string | null
  paths?: { fullchain: string; key: string }
}

interface SslState {
  enabled?: boolean
  domain?: string
  ca?: string
  autoRenew?: boolean
  renewalWindowDays?: number
  missing?: boolean
  message?: string
  certificates?: Certificate[]
  state?: { running: boolean; lastRunAt: string | null; lastError: string | null; lastOutput: string }
  error?: string
  data?: SslState
}

interface StackItem { layer: string; name: string; version: string }

const loading = ref(false)
const error = ref('')
const overview = ref<Record<string, unknown> | null>(null)
const hosts = computed<Host[]>(() => (overview.value?.hosts as Host[]) ?? [])
const ssl = computed<SslState>(() => {
  const raw = (overview.value?.ssl as SslState) ?? {}
  return raw.data ?? raw
})
const certificates = computed<Certificate[]>(() => ssl.value.certificates ?? [])
const stack = computed<StackItem[]>(() => (overview.value?.stack as StackItem[]) ?? [])
const summary = computed(() => (overview.value?.summary as Record<string, number>) ?? {})
const activeTab = ref('overview')
const autoRefresh = ref(false)
const renewing = ref(false)
const renewOutput = ref('')
const renewDialog = ref(false)
const actioning = ref('')

let timer: ReturnType<typeof setInterval> | null = null

async function load() {
  loading.value = true
  error.value = ''
  try {
    overview.value = await api.get<Record<string, unknown>>('/admin/v1/ops/overview')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '运维数据加载失败'
  } finally {
    loading.value = false
  }
}

function toggleAutoRefresh(value: boolean) {
  autoRefresh.value = value
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  if (value) {
    timer = setInterval(() => { void load() }, 30000)
  }
}

async function renew() {
  renewing.value = true
  renewOutput.value = ''
  renewDialog.value = true
  try {
    const result = await api.post<{ ok: boolean; data?: { output?: string } }>('/admin/v1/ops/ssl/renew', {})
    renewOutput.value = result.data?.output || '续签完成（未返回输出）'
    ElMessage.success('证书续签完成')
    await load()
  } catch (cause) {
    renewOutput.value = cause instanceof Error ? cause.message : '续签失败'
    ElMessage.error('证书续签失败')
  } finally {
    renewing.value = false
  }
}

async function act(container: Container, action: 'start' | 'stop' | 'restart', host: Host) {
  try {
    await ElMessageBox.confirm(
      '确定要 ' + action + ' 容器 ' + container.name + ' 吗？',
      '容器操作',
      { type: 'warning', confirmButtonText: '执行', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  actioning.value = host.name + '/' + container.id
  try {
    await api.post('/admin/v1/ops/containers/' + encodeURIComponent(host.name) + '/' + encodeURIComponent(container.id) + '/' + action)
    ElMessage.success('已提交 ' + action + ': ' + container.name)
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '容器操作失败')
  } finally {
    actioning.value = ''
  }
}

function bytes(value?: number | null) {
  if (value === null || value === undefined || !Number.isFinite(value)) return '-'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = value
  let unit = 0
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024
    unit++
  }
  return size.toFixed(size >= 100 ? 0 : 1) + ' ' + units[unit]
}

function duration(seconds?: number) {
  if (!seconds) return '-'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (days) return days + ' 天 ' + hours + ' 小时'
  if (hours) return hours + ' 小时 ' + minutes + ' 分钟'
  return minutes + ' 分钟'
}

function dateText(value?: string | null) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

function stateTag(state: string) {
  if (state === 'running') return 'success'
  if (state === 'paused') return 'warning'
  return 'info'
}

function severityTag(severity: string) {
  if (severity === 'high') return 'danger'
  if (severity === 'medium') return 'warning'
  return 'info'
}

function checkTag(status: string) {
  if (status === 'pass') return 'success'
  if (status === 'fail') return 'danger'
  if (status === 'warn') return 'warning'
  return 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = { pass: '通过', warn: '警告', fail: '失败', unknown: '未知' }
  return map[status] || status
}

function certTag(status: string) {
  if (status === 'valid') return 'success'
  if (status === 'expiring') return 'warning'
  return 'danger'
}

onMounted(load)
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>运维中心</h2>
        <p class="muted">服务器、技术栈、安全基线、辅助容器与 SSL 证书统一视图；支持局域网多机 agent 聚合。</p>
      </div>
      <div class="ops-actions">
        <el-switch v-model="autoRefresh" active-text="30s 自动刷新" @change="toggleAutoRefresh" />
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <div class="summary-grid">
      <div class="summary-card">
        <span>在线主机</span>
        <strong>{{ summary.hostsOnline ?? 0 }} / {{ summary.hosts ?? 0 }}</strong>
        <small>agent 聚合</small>
      </div>
      <div class="summary-card">
        <span>运行容器</span>
        <strong>{{ summary.containersRunning ?? 0 }} / {{ summary.containers ?? 0 }}</strong>
        <small>全部主机</small>
      </div>
      <div class="summary-card">
        <span>安全告警</span>
        <strong>{{ summary.securityWarnings ?? 0 }}</strong>
        <small>容器级风险项</small>
      </div>
      <div class="summary-card">
        <span>证书</span>
        <strong>{{ certificates.length ? certificates[0].daysLeft + ' 天' : '未配置' }}</strong>
        <small>{{ certificates[0]?.source || 'SSL 辅助容器' }}</small>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="总览" name="overview">
        <el-empty v-if="!hosts.length && !loading" description="没有配置 ops-agent（OPS_AGENTS）" />
        <div v-for="host in hosts" :key="host.name" class="host-card">
          <div class="host-head">
            <div>
              <h3>{{ host.name }} <el-tag size="small" :type="host.ok ? 'success' : 'danger'">{{ host.ok ? '在线' : '离线' }}</el-tag></h3>
              <p class="muted">{{ host.url }}</p>
            </div>
            <span v-if="host.host" class="muted">{{ host.host.hostname }} · {{ host.host.platform }} {{ host.host.release }} · {{ host.host.arch }}</span>
          </div>
          <el-alert v-if="!host.ok" :title="host.error || 'agent 不可达'" type="error" show-icon :closable="false" />
          <template v-else-if="host.host">
            <div class="host-metrics">
              <div><span>CPU</span><strong>{{ host.host.cpu.cores }} 核</strong><small>{{ host.host.cpu.model }}</small></div>
              <div>
                <span>内存</span><strong>{{ host.host.memory.usedPercent }}%</strong>
                <el-progress :percentage="host.host.memory.usedPercent" :show-text="false" />
                <small>{{ bytes(host.host.memory.usedBytes) }} / {{ bytes(host.host.memory.totalBytes) }}</small>
              </div>
              <div>
                <span>磁盘</span><strong>{{ host.host.disk.usedPercent }}%</strong>
                <el-progress :percentage="host.host.disk.usedPercent" :show-text="false" :status="host.host.disk.usedPercent > 90 ? 'exception' : undefined" />
                <small>{{ bytes(host.host.disk.usedBytes) }} / {{ bytes(host.host.disk.totalBytes) }}</small>
              </div>
              <div><span>运行时长</span><strong>{{ duration(host.host.uptimeSeconds) }}</strong><small>负载 {{ host.host.load.join(' / ') }}</small></div>
              <div><span>Docker</span><strong>{{ host.docker?.version || '-' }}</strong><small>{{ host.docker?.containersRunning ?? 0 }} 运行 / {{ host.docker?.containers ?? 0 }} 总数</small></div>
              <div><span>安全评分</span><strong>{{ host.security?.score ?? '-' }}</strong><small>{{ host.security?.summary.warn ?? 0 }} 警告 / {{ host.security?.summary.fail ?? 0 }} 失败</small></div>
            </div>
            <p class="muted">IP：{{ host.host.ips.map(item => item.interface + '=' + item.address).join('，') || '-' }} · 时区 {{ host.host.timezone }} · agent {{ host.host.agentVersion }}</p>
          </template>
        </div>

        <h3 class="section-title">技术栈</h3>
        <el-table :data="stack" size="small" border>
          <el-table-column prop="layer" label="层级" width="120" />
          <el-table-column prop="name" label="组件" />
          <el-table-column prop="version" label="版本 / 形态" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="容器" name="containers">
        <div v-for="host in hosts" :key="'c-' + host.name" class="host-card">
          <div class="host-head">
            <h3>{{ host.name }} <el-tag size="small" :type="host.ok ? 'success' : 'danger'">{{ host.ok ? '在线' : '离线' }}</el-tag></h3>
            <span class="muted">{{ host.containers?.length ?? 0 }} 个容器</span>
          </div>
          <el-alert v-if="!host.ok" :title="host.error || 'agent 不可达'" type="error" show-icon :closable="false" />
          <el-table v-else :data="host.containers || []" size="small" border row-key="id">
            <el-table-column prop="name" label="容器" min-width="180" />
            <el-table-column prop="image" label="镜像" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><el-tag size="small" :type="stateTag(row.state)">{{ row.state }}</el-tag></template>
            </el-table-column>
            <el-table-column label="CPU" width="90">
              <template #default="{ row }">{{ row.cpuPercent == null ? '-' : row.cpuPercent + '%' }}</template>
            </el-table-column>
            <el-table-column label="内存" width="140">
              <template #default="{ row }">
                <span v-if="row.memory">{{ bytes(row.memory.usageBytes) }} ({{ row.memory.percent }}%)</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="端口" min-width="140">
              <template #default="{ row }">
                <span v-if="row.ports?.length">{{ row.ports.map((p: any) => (p.publicPort ? p.publicPort + '→' : '') + p.privatePort + '/' + p.type).join('，') }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="风险" min-width="120">
              <template #default="{ row }">
                <el-tooltip v-if="row.securityWarnings?.length" :content="row.securityWarnings.join('；')">
                  <el-tag type="warning" size="small">{{ row.securityWarnings.length }} 项</el-tag>
                </el-tooltip>
                <span v-else class="muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="!row.managed" :loading="actioning === host.name + '/' + row.id" @click="act(row, 'restart', host)">重启</el-button>
                <el-button link type="warning" :disabled="!row.managed || row.state !== 'running'" @click="act(row, 'stop', host)">停止</el-button>
                <el-button link type="success" :disabled="!row.managed || row.state === 'running'" @click="act(row, 'start', host)">启动</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="安全" name="security">
        <div v-for="host in hosts" :key="'s-' + host.name" class="host-card">
          <div class="host-head">
            <h3>{{ host.name }} <el-tag size="small" :type="host.ok ? 'success' : 'danger'">{{ host.ok ? '在线' : '离线' }}</el-tag></h3>
            <span v-if="host.security" class="muted">评分 {{ host.security.score }} · 通过 {{ host.security.summary.pass }} · 警告 {{ host.security.summary.warn }} · 失败 {{ host.security.summary.fail }}</span>
          </div>
          <el-alert v-if="!host.ok" :title="host.error || 'agent 不可达'" type="error" show-icon :closable="false" />
          <template v-else>
            <el-table :data="host.security?.checks || []" size="small" border>
              <el-table-column label="级别" width="90">
                <template #default="{ row }"><el-tag size="small" :type="severityTag(row.severity)">{{ row.severity }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="title" label="检查项" min-width="160" />
              <el-table-column label="结果" width="90">
                <template #default="{ row }"><el-tag size="small" :type="checkTag(row.status)">{{ statusText(row.status) }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="evidence" label="证据" min-width="220" show-overflow-tooltip />
              <el-table-column prop="remediation" label="修复建议" min-width="260" show-overflow-tooltip />
            </el-table>
          </template>
        </div>
      </el-tab-pane>

      <el-tab-pane label="证书" name="ssl">
        <el-alert
          v-if="!ssl.enabled"
          :title="ssl.message || 'SSL 未启用'"
          type="info"
          show-icon
          :closable="false"
        />
        <template v-else>
          <div class="ssl-head">
            <div>
              <h3>
                {{ ssl.domain }}
                <el-tag size="small" :type="ssl.missing || certificates[0]?.source?.includes('自签名') ? 'warning' : 'success'">
                  {{ ssl.missing ? '未签发' : certificates[0]?.source?.includes('自签名') ? '自签名占位' : '已启用' }}
                </el-tag>
              </h3>
              <p class="muted">CA：{{ ssl.ca }} · 自动续签：{{ ssl.autoRenew ? '开启' : '关闭' }} · 提前 {{ ssl.renewalWindowDays }} 天续签</p>
            </div>
            <el-button type="primary" :loading="renewing" @click="renew">手工续签</el-button>
          </div>
          <el-alert v-if="ssl.state?.lastError" :title="'上次续签失败：' + ssl.state.lastError" type="error" show-icon :closable="false" class="mb-16" />
          <el-table :data="certificates" size="small" border>
            <el-table-column prop="domain" label="域名" min-width="160" />
            <el-table-column prop="source" label="证书来源" min-width="140" />
            <el-table-column prop="issuer" label="签发者" min-width="200" show-overflow-tooltip />
            <el-table-column label="生效时间" min-width="160">
              <template #default="{ row }">{{ dateText(row.validFrom) }}</template>
            </el-table-column>
            <el-table-column label="过期时间" min-width="160">
              <template #default="{ row }">{{ dateText(row.validTo) }}</template>
            </el-table-column>
            <el-table-column label="剩余" width="100">
              <template #default="{ row }"><el-tag size="small" :type="certTag(row.status)">{{ row.daysLeft }} 天</el-tag></template>
            </el-table-column>
            <el-table-column label="最近续签" min-width="160">
              <template #default="{ row }">{{ dateText(row.lastRenewAt) }}</template>
            </el-table-column>
          </el-table>
          <p v-if="!certificates.length" class="muted" style="margin-top: 12px">证书文件尚未生成。确认域名解析与 80 端口映射后，点击「手工续签」。</p>
        </template>
        <h3 class="section-title">证书来源说明</h3>
        <p class="muted">由 acme.sh 通过 HTTP-01 向 Let's Encrypt / ZeroSSL / Buypass 申请免费证书；续签后写入共享卷，边缘 Nginx 自动 reload。证书私钥保存在 ssl-certs 卷，admin 只展示元数据，不返回私钥。</p>
      </el-tab-pane>

      <el-tab-pane label="部署" name="deploy">
        <h3>辅助容器启动</h3>
        <pre class="code-block">cp .env.ops.example .env.ops
docker compose --env-file .env.ops -f compose.ops.yaml --profile edge up -d --build</pre>
        <h3>多机 agent</h3>
        <p class="muted">每台机器启动 ops（不需要 edge），然后在 API 配置：</p>
        <pre class="code-block">OPS_AGENTS=local=http://127.0.0.1:41287,edge=http://192.168.1.15:41287
OPS_TOKEN=与各 agent 的 OPS_TOKEN 一致</pre>
        <h3>端口映射检查</h3>
        <p class="muted">外网 80/443 → 边缘机 80/443；证书申请需要 80 可达。admin 的「域名配置」只填 host[:port]，不要带协议。</p>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="renewDialog" title="证书续签输出" width="720px">
      <pre class="code-block" style="max-height: 420px; overflow: auto">{{ renewOutput || '续签中...' }}</pre>
    </el-dialog>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.ops-actions { display: flex; align-items: center; gap: 12px; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.summary-card { border: 1px solid #ece9e2; border-radius: 10px; padding: 14px 16px; background: #fff; display: flex; flex-direction: column; gap: 4px; }
.summary-card span { color: #8a857c; font-size: 12px; }
.summary-card strong { font-size: 22px; }
.summary-card small { color: #a19b90; }
.host-card { border: 1px solid #ece9e2; border-radius: 12px; padding: 16px; margin-bottom: 16px; background: #fff; }
.host-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 12px; }
.host-head h3 { margin: 0 0 4px; }
.host-head p { margin: 0; }
.host-metrics { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 12px; margin-bottom: 12px; }
.host-metrics > div { border: 1px solid #f0ede6; border-radius: 8px; padding: 10px; display: flex; flex-direction: column; gap: 4px; }
.host-metrics span { font-size: 12px; color: #8a857c; }
.host-metrics strong { font-size: 18px; }
.host-metrics small { color: #a19b90; font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.section-title { margin: 20px 0 10px; }
.ssl-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; }
.ssl-head h3 { margin: 0 0 4px; }
.code-block { background: #1f2430; color: #e6e6e6; padding: 12px 14px; border-radius: 8px; font-size: 12px; line-height: 1.6; overflow-x: auto; }
@media (max-width: 1100px) {
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .host-metrics { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
</style>
