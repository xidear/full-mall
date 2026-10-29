<script setup lang="ts">
/**
 * 市场配置（ADR-0014）：市场 / 语言 / 收货地是三个正交维度。
 * 这一页只编辑「市场」这一轴——币种、税辖区、法律主体、语言候选、市场级设置；
 * 收货地（地址国家）在顾客结算时决定，不在这里配置。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface MarketView {
  code: string
  name: string
  currency: string
  countryCode: string | null
  taxJurisdiction: string
  priceIncludesTax: boolean | null
  timezone: string
  legalEntityName: string | null
  active: boolean
  sort: number
  locales: string[]
  settings: Record<string, string>
}

interface IntegrationRow {
  marketCode: string
  capability: string
  provider: string
  enabled: boolean
}

const markets = ref<MarketView[]>([])
const loading = ref(false)
const error = ref('')
const saving = ref(false)

/**
 * 通道按市场启用（ADR-0014 §8.4）：这里只管"哪个市场用哪个服务商"，
 * 凭据仍在「第三方服务」页。同市场同能力只能启用一个——切换由后端负责关旧的。
 */
const integrations = ref<IntegrationRow[]>([])
const integrationSaving = ref(false)
const newIntegration = reactive({ capability: 'payment', provider: '' })
const CAPABILITIES = ['payment', 'logistics', 'sms', 'storage', 'moderation', 'ai', 'address']

const dialogVisible = ref(false)
const editing = reactive({
  code: '',
  name: '',
  currency: '',
  countryCode: '',
  taxJurisdiction: '',
  priceIncludesTax: true as boolean | null,
  timezone: '',
  legalEntityName: '',
  active: true,
  sort: 0,
  locales: '',
  primaryLocale: '',
  icpNumber: '',
  servicePhone: ''
})

async function loadIntegrations(code: string) {
  integrations.value = code
    ? await api.get<IntegrationRow[]>(`/admin/v1/markets/${encodeURIComponent(code)}/integrations`)
    : []
}

async function toggleIntegration(row: IntegrationRow, enabled: boolean) {
  integrationSaving.value = true
  try {
    integrations.value = await api.put<IntegrationRow[]>(
      `/admin/v1/markets/${encodeURIComponent(row.marketCode)}/integrations`,
      { capability: row.capability, provider: row.provider, enabled })
    ElMessage.success(enabled ? '已启用' : '已停用')
  } catch (cause) {
    ElMessage.error('操作失败：' + (cause as Error).message)
  } finally {
    integrationSaving.value = false
  }
}

async function addIntegration() {
  if (!editing.code.trim()) { ElMessage.error('请先保存市场'); return }
  if (!newIntegration.provider.trim()) { ElMessage.error('请填服务商标识'); return }
  integrationSaving.value = true
  try {
    integrations.value = await api.put<IntegrationRow[]>(
      `/admin/v1/markets/${encodeURIComponent(editing.code.trim())}/integrations`,
      { capability: newIntegration.capability, provider: newIntegration.provider.trim(), enabled: true })
    newIntegration.provider = ''
    ElMessage.success('已启用')
  } catch (cause) {
    ElMessage.error('保存失败：' + (cause as Error).message)
  } finally {
    integrationSaving.value = false
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    markets.value = await api.get<MarketView[]>('/admin/v1/markets')
  } catch (cause) {
    error.value = '市场列表加载失败：' + (cause as Error).message
  } finally {
    loading.value = false
  }
}

function openEdit(market: MarketView) {
  editing.code = market.code
  editing.name = market.name
  editing.currency = market.currency
  editing.countryCode = market.countryCode ?? ''
  editing.taxJurisdiction = market.taxJurisdiction
  editing.priceIncludesTax = market.priceIncludesTax
  editing.timezone = market.timezone
  editing.legalEntityName = market.legalEntityName ?? ''
  editing.active = market.active
  editing.sort = market.sort
  editing.locales = market.locales.join(',')
  editing.primaryLocale = market.locales[0] ?? ''
  editing.icpNumber = market.settings.icp_number ?? ''
  editing.servicePhone = market.settings.service_phone ?? ''
  void loadIntegrations(market.code)
  dialogVisible.value = true
}

function openCreate() {
  Object.assign(editing, {
    code: '', name: '', currency: 'CNY', countryCode: '', taxJurisdiction: '',
    priceIncludesTax: true, timezone: '', legalEntityName: '', active: true, sort: 0,
    locales: '', primaryLocale: '', icpNumber: '', servicePhone: ''
  })
  integrations.value = []
  dialogVisible.value = true
}

async function save() {
  if (!editing.code.trim()) {
    ElMessage.error('市场码不能为空')
    return
  }
  // 含税口径与税辖区必须显式声明：留空会让税额算错（后端同样强制，见 MarketService）
  if (editing.priceIncludesTax === null) {
    ElMessage.error('请选择标价是否含税')
    return
  }
  saving.value = true
  try {
    await api.put(`/admin/v1/markets/${encodeURIComponent(editing.code.trim())}`, {
      name: editing.name,
      currency: editing.currency,
      countryCode: editing.countryCode || null,
      taxJurisdiction: editing.taxJurisdiction,
      priceIncludesTax: editing.priceIncludesTax,
      timezone: editing.timezone,
      legalEntityName: editing.legalEntityName || null,
      active: editing.active,
      sort: editing.sort
    })
    const locales = editing.locales.split(',').map(x => x.trim()).filter(Boolean)
    if (locales.length) {
      await api.put(`/admin/v1/markets/${encodeURIComponent(editing.code.trim())}/locales`, {
        locales,
        primaryLocale: editing.primaryLocale || locales[0]
      })
    }
    // 两个键**总是**写（包括空串）：留空表示"该市场没有这个值"，
    // 只在非空时才写会让"清空"变成"沿用全局"，香港站又会显示大陆备案号。
    await api.put(`/admin/v1/markets/${encodeURIComponent(editing.code.trim())}/settings`, {
      icp_number: editing.icpNumber,
      service_phone: editing.servicePhone
    })
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } catch (cause) {
    ElMessage.error('保存失败：' + (cause as Error).message)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="markets-page">
    <div class="page-heading">
      <h1>市场配置</h1>
      <div class="actions">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新建市场</el-button>
      </div>
    </div>

    <p class="hint">
      市场是「谁在卖」：币种、税辖区、法律主体、可用语言。它<b>不等于</b>顾客所在国家——
      同一商品寄到不同国家由收货地决定运费与税。
    </p>

    <el-alert v-if="error" :title="error" type="error" :closable="false" />

    <el-table :data="markets" v-loading="loading" empty-text="暂无市场">
      <el-table-column prop="code" label="市场码" width="100" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="currency" label="币种" width="90" />
      <el-table-column prop="countryCode" label="国家" width="90" />
      <el-table-column prop="taxJurisdiction" label="税务辖区" width="130" />
      <el-table-column label="含税价" width="90">
        <template #default="{ row }">
          {{ row.priceIncludesTax === null ? '未配置' : row.priceIncludesTax ? '含税' : '净价' }}
        </template>
      </el-table-column>
      <el-table-column prop="timezone" label="时区" min-width="140" />
      <el-table-column label="语言候选" min-width="180">
        <template #default="{ row }">{{ row.locales.join(' / ') || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '已启用' : '已停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing.code ? `编辑市场 ${editing.code}` : '新建市场'" width="640px">
      <el-form label-width="110px">
        <el-form-item label="市场码">
          <el-input v-model="editing.code" :disabled="!!editing.code" placeholder="如 JP / HK / US" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="editing.name" placeholder="如 日本" />
        </el-form-item>
        <el-form-item label="币种">
          <el-input v-model="editing.currency" placeholder="ISO-4217，如 JPY" />
        </el-form-item>
        <el-form-item label="国家">
          <el-input v-model="editing.countryCode" placeholder="ISO 3166-1 alpha-2，如 JP" />
        </el-form-item>
        <el-form-item label="税务辖区">
          <el-input v-model="editing.taxJurisdiction" placeholder="如 JP / EU-OSS" />
        </el-form-item>
        <el-form-item label="标价含税">
          <el-radio-group v-model="editing.priceIncludesTax">
            <el-radio :value="true">含税价</el-radio>
            <el-radio :value="false">净价</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="时区">
          <el-input v-model="editing.timezone" placeholder="IANA，如 Asia/Tokyo" />
        </el-form-item>
        <el-form-item label="法律主体">
          <el-input v-model="editing.legalEntityName" placeholder="如 XX 株式会社" />
        </el-form-item>
        <el-form-item label="语言候选">
          <el-input v-model="editing.locales" placeholder="逗号分隔，如 ja,en" />
        </el-form-item>
        <el-form-item label="主语言">
          <el-input v-model="editing.primaryLocale" placeholder="必须在语言候选内，如 ja" />
        </el-form-item>
        <el-form-item label="ICP 备案">
          <el-input v-model="editing.icpNumber" placeholder="该市场的备案号" />
        </el-form-item>
        <el-form-item label="客服电话">
          <el-input v-model="editing.servicePhone" placeholder="该市场的客服电话" />
        </el-form-item>
        <el-form-item label=" ">
          <span class="field-hint">
            留空 = 该市场**不展示**该项（不会回退到全局值）；想沿用全局请填与全局相同的值。
          </span>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editing.sort" :min="0" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editing.active" />
        </el-form-item>

        <el-form-item label="通道启用">
          <div class="integration-list">
            <div v-for="row in integrations" :key="row.capability + row.provider" class="integration-row">
              <span class="cap">{{ row.capability }}</span>
              <span class="prov">{{ row.provider }}</span>
              <el-switch
                :model-value="row.enabled"
                :loading="integrationSaving"
                @change="(value: string | number | boolean) => toggleIntegration(row, Boolean(value))"
              />
            </div>
            <div class="integration-row">
              <el-select v-model="newIntegration.capability" style="width: 120px">
                <el-option v-for="cap in CAPABILITIES" :key="cap" :label="cap" :value="cap" />
              </el-select>
              <el-input v-model="newIntegration.provider" placeholder="服务商标识，如 stripe" style="flex: 1" />
              <el-button :loading="integrationSaving" @click="addIntegration">添加并启用</el-button>
            </div>
            <p class="field-hint">
              同一能力的服务商只能启用一个；凭据请在「第三方服务」页配置。未配置任何通道的市场，
              能力查询会显示 paymentEnabled=false——下单门禁不拦它，但支付环节会失败。
            </p>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.hint { color: var(--text-muted, #666); margin: 0 0 16px; line-height: 1.6; }
.integration-list { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.integration-row { display: flex; align-items: center; gap: 8px; }
.integration-row .cap { width: 96px; color: var(--text-muted, #666); }
.integration-row .prov { flex: 1; }
.field-hint { color: var(--text-muted, #666); font-size: 12px; margin: 4px 0 0; line-height: 1.5; }
</style>
