<script setup lang="ts">
import { formatDateTime } from '@fullmall/api-client'
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { SupplierCredentialView } from '@fullmall/api-client'
import { api } from '../api'
import StatusToggle from '../components/StatusToggle.vue'
import { useEnums } from '../composables/useEnums'

const { enumLabel } = useEnums()

const suppliers = ref<SupplierCredentialView[]>([])
const sortKey = ref('name')
const sortDirection = ref<'asc' | 'desc'>('asc')

function onSortChange(payload: { prop: string | null; order: 'ascending' | 'descending' | null }) {
  sortKey.value = payload.prop ?? 'name'
  sortDirection.value = payload.order === 'descending' ? 'desc' : 'asc'
  load()
}
const loading = ref(false)
const error = ref('')
const credentialDialog = ref(false)
// 供应商结算：周期内生成结算单，已结算退货会从应付中扣减
interface SettlementRow {
  id: string
  periodStart: string
  periodEnd: string
  grossAmount: number
  refundAmount: number
  commissionAmount: number
  payableAmount: number
  status: string
}
const settlementDialog = ref(false)
const settlementLoading = ref(false)
const settlementSaving = ref(false)
const settlementSupplier = ref<SupplierCredentialView | null>(null)
const settlements = ref<SettlementRow[]>([])
const settlementRange = ref<Date[] | null>(null)

async function openSettlements(row: SupplierCredentialView) {
  settlementSupplier.value = row
  settlementDialog.value = true
  settlementRange.value = null
  await loadSettlements()
}

async function loadSettlements() {
  if (!settlementSupplier.value) return
  settlementLoading.value = true
  try {
    settlements.value = await api.get<SettlementRow[]>(
      `/admin/v1/suppliers/${settlementSupplier.value.supplierId}/settlements`, { limit: 20 })
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '结算单加载失败')
  } finally {
    settlementLoading.value = false
  }
}

async function generateSettlement() {
  if (!settlementSupplier.value || settlementSaving.value) return
  if (!settlementRange.value || settlementRange.value.length !== 2) {
    ElMessage.error('请选择结算周期')
    return
  }
  settlementSaving.value = true
  try {
    await api.post(
      `/admin/v1/suppliers/${settlementSupplier.value.supplierId}/settlements`
        + `?periodStart=${encodeURIComponent(new Date(settlementRange.value[0]).toISOString())}`
        + `&periodEnd=${encodeURIComponent(new Date(settlementRange.value[1]).toISOString())}`)
    ElMessage.success('结算单已生成')
    await loadSettlements()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '生成失败')
  } finally {
    settlementSaving.value = false
  }
}
const created = ref<{ supplierId: string; accessKey: string; secret: string } | null>(null)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    suppliers.value = await api.get<SupplierCredentialView[]>('/admin/v1/suppliers',
      { sort: sortKey.value, direction: sortDirection.value })
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '供应商加载失败'
  } finally {
    loading.value = false
  }
}

/** 只切换启用状态；失败抛出让开关保持原值。 */
async function toggleActive(row: { supplierId?: string; id?: string; active: boolean }, next: boolean) {
  const id = row.supplierId ?? row.id
  await api.post(`/admin/v1/suppliers/` + id + `/active`, { active: next })
  row.active = next
}

async function rotate(supplier: SupplierCredentialView) {
  loading.value = true
  error.value = ''
  try {
    created.value = await api.post<{ supplierId: string; accessKey: string; secret: string }>(
      `/admin/v1/suppliers/${supplier.supplierId}/credentials`
    )
    credentialDialog.value = true
    await load()
    ElMessage.success('凭证已生成，旧凭证立即失效')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '凭证生成失败'
  } finally {
    loading.value = false
  }
}

async function copy(text: string, label: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`${label}已复制`)
  } catch {
    ElMessage.warning('浏览器拒绝剪贴板访问，请手动选择复制')
  }
}
</script>

<template>
  <div v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <div class="panel">
      <div class="panel-header">
        <div>
          <h2>供应商与开放接口凭证</h2>
          <p class="muted">
            供应商使用 Access Key + Secret 调用 `/open/v1`；每个请求使用 HMAC-SHA256 签名。
            Secret 只在生成时显示一次。
          </p>
        </div>
        <el-button @click="load">刷新</el-button>
      </div>

      <el-table :data="suppliers" @sort-change="onSortChange" empty-text="暂无供应商">
        <el-table-column prop="code" sortable="custom" label="供应商编码" width="150" />
        <el-table-column prop="name" sortable="custom" label="供应商名称" min-width="180" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <!-- 列表内直接切换：停用后该供应商凭证立即失效 -->
            <StatusToggle :active="row.active" active-text="启用" inactive-text="停用"
                          :confirm-text="'停用供应商 ' + row.name + ' 后其接口凭证立即不可用，确定吗？'"
                          :on-change="next => toggleActive(row, next)" />
          </template>
        </el-table-column>
        <el-table-column label="Access Key" min-width="240">
          <template #default="{ row }">
            <code>{{ row.accessKey || '尚未生成' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="最近轮换" width="190">
          <template #default="{ row }">
            {{ row.rotatedAt ? formatDateTime(row.rotatedAt) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="rotate(row)">
              {{ row.hasCredential ? '轮换凭证' : '生成凭证' }}
            </el-button>
            <el-button size="small" plain @click="openSettlements(row)">结算</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="settlementDialog" title="供应商结算" width="720px" v-loading="settlementLoading">
      <div class="header-actions" style="margin-bottom: 12px">
        <span class="muted">周期</span>
        <el-date-picker v-model="settlementRange" type="datetimerange" style="width: 340px" />
        <el-button type="primary" :loading="settlementSaving" @click="generateSettlement">生成结算单</el-button>
      </div>
      <el-table :data="settlements" size="small" empty-text="暂无结算单">
        <el-table-column label="周期" min-width="220">
          <template #default="{ row }">
            {{ formatDateTime(row.periodStart) }} ~ {{ formatDateTime(row.periodEnd) }}
          </template>
        </el-table-column>
        <el-table-column label="毛额" width="100">
          <template #default="{ row }">{{ (row.grossAmount / 100).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="退货扣减" width="110">
          <template #default="{ row }">{{ (row.refundAmount / 100).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="佣金" width="100">
          <template #default="{ row }">{{ (row.commissionAmount / 100).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="应付" width="110">
          <template #default="{ row }"><strong>{{ (row.payableAmount / 100).toFixed(2) }}</strong></template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">{{ enumLabel('SUPPLIER_STATUS', row.status) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="credentialDialog" title="供应商凭证（仅显示一次）" width="620px">
      <el-alert
        title="请立即保存 Secret。关闭后服务端只能验证，无法再次显示明文。"
        type="warning"
        show-icon
        :closable="false"
        class="mb-16"
      />
      <div v-if="created" class="credential-block">
        <div class="credential-row">
          <span>Access Key</span>
          <code>{{ created.accessKey }}</code>
          <el-button size="small" @click="copy(created.accessKey, 'Access Key')">复制</el-button>
        </div>
        <div class="credential-row">
          <span>Secret</span>
          <code>{{ created.secret }}</code>
          <el-button size="small" @click="copy(created.secret, 'Secret')">复制</el-button>
        </div>
      </div>
      <el-alert
        title="供应商门户 http://localhost:5175 输入这两个值即可连接；正式环境请通过 HTTPS 使用。"
        type="info"
        :closable="false"
      />
      <template #footer>
        <el-button type="primary" @click="credentialDialog = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.credential-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.credential-row {
  display: grid;
  grid-template-columns: 100px 1fr auto;
  align-items: center;
  gap: 10px;
}

.credential-row code {
  overflow-wrap: anywhere;
  padding: 8px 10px;
  border-radius: 6px;
  background: #f6f4ee;
  font-size: 12px;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>
