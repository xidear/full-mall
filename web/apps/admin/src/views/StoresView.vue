<script setup lang="ts">
/**
 * 门店管理：门店资料 + 启停。
 *
 * <p>状态只放在列表内联开关里（AGENTS §13.1）：操作列不再出现重复的「启用/停用」按钮，
 * 失败由 StatusToggle 保持原状态并报错。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import StatusToggle from '../components/StatusToggle.vue'

interface StoreRow {
  id: string
  code: string
  marketCode: string
  name: string
  address: string
  latitude?: number | null
  longitude?: number | null
  pickupEnabled: boolean
  active: boolean
  phone?: string | null
  openingHours?: string | null
  createdAt: string
}

interface MarketRow { code: string; name: string; active: boolean }

const stores = ref<StoreRow[]>([])
const markets = ref<MarketRow[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const keyword = ref('')
const marketFilter = ref('')
const statusFilter = ref<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL')
const dialog = ref(false)
const editingId = ref('')

const form = reactive({
  code: '',
  marketCode: '',
  name: '',
  address: '',
  latitude: undefined as number | undefined,
  longitude: undefined as number | undefined,
  pickupEnabled: true,
  phone: '',
  openingHours: ''
})

const filtered = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return stores.value.filter(row => {
    if (marketFilter.value && row.marketCode !== marketFilter.value) return false
    if (statusFilter.value === 'ACTIVE' && !row.active) return false
    if (statusFilter.value === 'INACTIVE' && row.active) return false
    if (!text) return true
    return [row.code, row.name, row.address, row.phone ?? '']
      .some(value => value.toLowerCase().includes(text))
  })
})

const marketName = computed(() => {
  const map = new Map(markets.value.map(market => [market.code, market.name]))
  return (code: string) => map.get(code) ?? code
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [storeList, marketList] = await Promise.all([
      api.get<StoreRow[]>('/admin/v1/stores', { includeInactive: true }),
      api.get<MarketRow[]>('/admin/v1/markets')
    ])
    stores.value = storeList
    markets.value = marketList
    if (!form.marketCode) {
      form.marketCode = marketList.find(market => market.active)?.code ?? marketList[0]?.code ?? ''
    }
  } catch (cause) {
    error.value = '门店加载失败：' + (cause instanceof Error ? cause.message : String(cause))
  } finally {
    loading.value = false
  }
}

async function generateCode() {
  try {
    const result = await api.get<{ code: string }>('/admin/v1/codes/suggest', { type: 'STORE' })
    form.code = result.code
  } catch (cause) {
    ElMessage.error('生成编码失败：' + (cause instanceof Error ? cause.message : String(cause)))
  }
}

function openCreate() {
  editingId.value = ''
  form.code = ''
  form.marketCode = markets.value.find(market => market.active)?.code ?? ''
  form.name = ''
  form.address = ''
  form.latitude = undefined
  form.longitude = undefined
  form.pickupEnabled = true
  form.phone = ''
  form.openingHours = ''
  dialog.value = true
}

function openEdit(row: StoreRow) {
  editingId.value = row.id
  form.code = row.code
  form.marketCode = row.marketCode
  form.name = row.name
  form.address = row.address
  form.latitude = row.latitude ?? undefined
  form.longitude = row.longitude ?? undefined
  form.pickupEnabled = row.pickupEnabled
  form.phone = row.phone ?? ''
  form.openingHours = row.openingHours ?? ''
  dialog.value = true
}

/**
 * 前端只做"提交前拦明显错误"，权威校验在后端：
 * 编码格式、市场是否存在、编码唯一性都以服务端错误码为准（AGENTS §9.2）。
 */
async function submit() {
  if (saving.value) return
  const code = form.code.trim().toUpperCase()
  if (!/^[A-Z0-9][A-Z0-9_-]{1,63}$/.test(code)) {
    ElMessage.error('门店编码只能是大写字母/数字/-/_，长度 2..64')
    return
  }
  if (!form.marketCode) { ElMessage.error('请选择所属市场'); return }
  if (!form.name.trim()) { ElMessage.error('请填写门店名称'); return }
  if (!form.address.trim()) { ElMessage.error('请填写门店地址'); return }
  if ((form.latitude == null) !== (form.longitude == null)) {
    ElMessage.error('经纬度必须同时填写或同时留空')
    return
  }
  const payload = {
    code,
    marketCode: form.marketCode,
    name: form.name.trim(),
    address: form.address.trim(),
    latitude: form.latitude ?? null,
    longitude: form.longitude ?? null,
    pickupEnabled: form.pickupEnabled,
    phone: form.phone.trim() || null,
    openingHours: form.openingHours.trim() || null
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.put(`/admin/v1/stores/${editingId.value}`, payload)
      ElMessage.success('门店已保存')
    } else {
      await api.post('/admin/v1/stores', payload)
      ElMessage.success('门店已创建')
    }
    dialog.value = false
    await load()
  } catch (cause) {
    error.value = '保存门店失败：' + (cause instanceof Error ? cause.message : String(cause))
  } finally {
    saving.value = false
  }
}

async function toggleActive(row: StoreRow, next: boolean) {
  const saved = await api.post<StoreRow>(`/admin/v1/stores/${row.id}/active`, { active: next })
  Object.assign(row, saved)
}

onMounted(load)
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>门店管理</h2>
        <p class="muted">
          维护门店资料与营业状态；停用后顾客不能再选择该门店自提，但历史订单与库存记录仍保留。
        </p>
      </div>
      <div class="header-actions">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新建门店</el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <div class="filters">
      <el-input v-model="keyword" placeholder="搜索编码 / 名称 / 地址 / 电话" clearable style="width: 260px" />
      <el-select v-model="marketFilter" placeholder="全部市场" clearable style="width: 180px">
        <el-option v-for="market in markets" :key="market.code"
                   :label="market.name + '（' + market.code + '）'" :value="market.code" />
      </el-select>
      <el-select v-model="statusFilter" style="width: 140px">
        <el-option label="全部状态" value="ALL" />
        <el-option label="营业中" value="ACTIVE" />
        <el-option label="已停用" value="INACTIVE" />
      </el-select>
      <span class="muted">共 {{ filtered.length }} 家</span>
    </div>

    <el-table :data="filtered" stripe empty-text="暂无门店">
      <el-table-column prop="code" label="编码" width="180" />
      <el-table-column label="门店" min-width="220">
        <template #default="{ row }">
          <strong>{{ row.name }}</strong>
          <div class="muted">{{ row.address }}</div>
        </template>
      </el-table-column>
      <el-table-column label="市场" width="140">
        <template #default="{ row }">{{ marketName(row.marketCode) }}</template>
      </el-table-column>
      <el-table-column label="电话 / 营业时间" min-width="180">
        <template #default="{ row }">
          <div>{{ row.phone || '—' }}</div>
          <div class="muted">{{ row.openingHours || '未填写营业时间' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="自提" width="100">
        <template #default="{ row }">
          <el-tag :type="row.pickupEnabled ? 'success' : 'info'">
            {{ row.pickupEnabled ? '支持' : '不支持' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <StatusToggle :active="row.active" active-text="营业中" inactive-text="已停用"
                        :confirm-text="`停用门店「${row.name}」后，顾客将不能选择该门店自提；历史订单与库存记录保留。确定吗？`"
                        :on-change="next => toggleActive(row, next)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editingId ? '编辑门店' : '新建门店'" width="620px">
      <el-form label-width="110px">
        <el-form-item label="门店编码">
          <div style="display: flex; gap: 8px; width: 100%">
            <el-input v-model="form.code" maxlength="64" placeholder="例如 STORE-SH-001" />
            <el-button @click="generateCode">生成</el-button>
          </div>
        </el-form-item>
        <el-form-item label="所属市场">
          <el-select v-model="form.marketCode" filterable placeholder="选择市场" style="width: 100%">
            <el-option v-for="market in markets" :key="market.code"
                       :label="market.name + '（' + market.code + '）'" :value="market.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="门店名称">
          <el-input v-model="form.name" maxlength="128" placeholder="例如 上海静安店" />
        </el-form-item>
        <el-form-item label="门店地址">
          <el-input v-model="form.address" maxlength="255" placeholder="详细地址（顾客自提时会看到）" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" maxlength="32" placeholder="数字、+、-、空格，可留空" />
        </el-form-item>
        <el-form-item label="营业时间">
          <el-input v-model="form.openingHours" maxlength="128" placeholder="例如 09:00-22:00" />
        </el-form-item>
        <el-form-item label="纬度 / 经度">
          <div style="display: flex; gap: 8px; width: 100%">
            <el-input-number v-model="form.latitude" :precision="6" :step="0.0001"
                             :min="-90" :max="90" controls-position="right" style="width: 50%" />
            <el-input-number v-model="form.longitude" :precision="6" :step="0.0001"
                             :min="-180" :max="180" controls-position="right" style="width: 50%" />
          </div>
          <div class="muted" style="width: 100%">用于「附近门店」排序；同时留空表示不参与距离计算。</div>
        </el-form-item>
        <el-form-item label="支持自提">
          <el-switch v-model="form.pickupEnabled" active-text="允许顾客选择到店自提" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header-actions {
  display: flex;
  gap: 8px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>