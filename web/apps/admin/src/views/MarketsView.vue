<script setup lang="ts">
/**
 * 市场配置（ADR-0014）：市场 / 语言 / 收货地是三个正交维度。
 * 这一页只编辑「市场」这一轴——币种、税辖区、法律主体、语言候选、市场级设置；
 * 收货地（地址国家）在顾客结算时决定，不在这里配置。
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
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

interface LocaleView {
  defaultLocale: string
  enabledLocales: string[]
  translationLocales: string[]
}

interface RegionOption {
  code: string
  name: string
}

interface CapabilitySchema {
  code: string
  label: string
  providers: Array<{ code: string; label: string }>
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
/** 能力 → 可选服务商，全部来自后端接口（AGENTS §13：枚举禁止手填，页面里不再写死候选）。 */
const catalog = ref<Record<string, string[]>>({})
const capabilities = computed(() => Object.keys(catalog.value))
const providersForCapability = computed(() => catalog.value[newIntegration.capability] ?? [])

// 候选全部来自后端/浏览器内置能力（AGENTS §13）：币种与 IANA 时区用 Intl 的 ISO 清单，
// 语言用后台已启用的语言，国家用 region 数据，通道用第三方服务清单——页面里不写死任何候选。
const localeOptions = ref<string[]>([])
const countryOptions = ref<RegionOption[]>([])
/** 税务辖区候选来自已建的税率规则（后端去重查询），不是前端写死的清单。 */
const jurisdictionOptions = ref<string[]>([])
const currencyOptions = ref<string[]>([])
const timezoneOptions = ref<string[]>([])
const integrationSchema = ref<CapabilitySchema[]>([])
const optionError = ref('')

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
  locales: [] as string[],
  primaryLocale: '',
  icpNumber: '',
  servicePhone: ''
})

/**
 * 加载所有可枚举候选。失败时**不静默**：页面顶部会显示错误，且相关下拉为空（保存前会被拦下）。
 */
async function loadOptions() {
  optionError.value = ''
  try {
    const [locales, countries, schema] = await Promise.all([
      api.get<LocaleView>('/admin/v1/i18n/locales'),
      api.get<RegionOption[]>('/api/v1/regions/countries', { locale: 'zh-CN' }),
      api.get<CapabilitySchema[]>('/admin/v1/integrations/schema')
    ])
    localeOptions.value = [...new Set([...locales.translationLocales, ...locales.enabledLocales])]
    countryOptions.value = countries
    integrationSchema.value = schema
    jurisdictionOptions.value = await api.get<string[]>('/admin/v1/tax-rules/jurisdictions')
  } catch (cause) {
    optionError.value = '候选清单加载失败（币种/时区仍可用，语言/国家/通道需重新加载）：' + (cause as Error).message
  }
  // 币种与 IANA 时区用浏览器内置的 ISO 清单：不引依赖，也不会和后端数据不一致
  const intlWithValues = Intl as typeof Intl & { supportedValuesOf?: (key: string) => string[] }
  currencyOptions.value = intlWithValues.supportedValuesOf?.('currency') ?? []
  timezoneOptions.value = intlWithValues.supportedValuesOf?.('timeZone') ?? []
  if (currencyOptions.value.length === 0 || timezoneOptions.value.length === 0) {
    optionError.value = '当前浏览器不支持 Intl.supportedValuesOf，币种/时区无法给出候选；请使用新版 Chrome/Edge/Safari'
  }
}

function capabilityLabel(code: string): string {
  return integrationSchema.value.find(item => item.code === code)?.label ?? code
}

function providerLabel(capability: string, provider: string): string {
  return integrationSchema.value.find(item => item.code === capability)?.providers.find(item => item.code === provider)?.label ?? provider
}

// 主语言必须在候选内：取消勾选后自动回落到第一个候选，避免提交出"主语言不在候选里"的无效组合
watch(() => editing.locales, value => {
  if (!value.includes(editing.primaryLocale)) {
    editing.primaryLocale = value[0] ?? ''
  }
})

async function loadIntegrationCatalog() {
  catalog.value = await api.get<Record<string, string[]>>('/admin/v1/markets/integration-catalog')
  if (!catalog.value[newIntegration.capability]) {
    const first = Object.keys(catalog.value)[0]
    if (first) newIntegration.capability = first
  }
  if (!providersForCapability.value.includes(newIntegration.provider)) {
    newIntegration.provider = providersForCapability.value[0] ?? ''
  }
}

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
  editing.locales = [...market.locales]
  editing.primaryLocale = market.locales[0] ?? ''
  editing.icpNumber = market.settings.icp_number ?? ''
  editing.servicePhone = market.settings.service_phone ?? ''
  void loadIntegrationCatalog()
  void loadIntegrations(market.code)
  dialogVisible.value = true
}

function openCreate() {
  Object.assign(editing, {
    code: '', name: '', currency: 'CNY', countryCode: '', taxJurisdiction: '',
    priceIncludesTax: true, timezone: '', legalEntityName: '', active: true, sort: 0,
    locales: [], primaryLocale: '', icpNumber: '', servicePhone: ''
  })
  integrations.value = []
  void loadIntegrationCatalog()
  dialogVisible.value = true
}

async function save() {
  if (!editing.code.trim()) {
    ElMessage.error('市场码不能为空')
    return
  }
  // 含税口径与税辖区必须显式声明：留空会让税额算错（后端同样强制，见 MarketService）
  // 后端 non_null 序列化会把未配置的字段整个省略，前端拿到 undefined：必须用 == null
  if (editing.priceIncludesTax === null || editing.priceIncludesTax === undefined) {
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
    const locales = editing.locales.filter(Boolean)
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

onMounted(async () => {
  await loadOptions()
  await load()
})
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
    <el-alert v-if="optionError" :title="optionError" type="warning" :closable="false" class="mb-12" />

    <el-table :data="markets" v-loading="loading" empty-text="暂无市场">
      <el-table-column prop="code" label="市场码" width="100" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="currency" label="币种" width="90" />
      <el-table-column prop="countryCode" label="国家" width="90" />
      <el-table-column prop="taxJurisdiction" label="税务辖区" width="130" />
      <el-table-column label="含税价" width="90">
        <template #default="{ row }">
          {{ row.priceIncludesTax == null ? '未配置' : row.priceIncludesTax ? '含税' : '净价' }}
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
          <el-select v-model="editing.currency" filterable placeholder="选择币种（ISO-4217）" style="width: 100%">
            <el-option v-for="currency in currencyOptions" :key="currency" :label="currency" :value="currency" />
          </el-select>
        </el-form-item>
        <el-form-item label="国家">
          <el-select v-model="editing.countryCode" filterable clearable placeholder="选择国家/地区（可留空）" style="width: 100%">
            <el-option v-for="country in countryOptions" :key="country.code"
                       :label="country.name + '（' + country.code + '）'" :value="country.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="税务辖区">
          <el-select v-model="editing.taxJurisdiction" filterable allow-create default-first-option
                     placeholder="选择已建过税率规则的辖区（也可输入新代码）" style="width: 100%">
            <el-option v-for="item in jurisdictionOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <span class="field-hint">
            候选来自「税率规则」里已建的辖区；空列表说明还没建过规则（先建规则，或在此输入新代码）。
          </span>
        </el-form-item>
        <el-form-item label="标价含税">
          <el-radio-group v-model="editing.priceIncludesTax">
            <el-radio :value="true">含税价</el-radio>
            <el-radio :value="false">净价</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="时区">
          <el-select v-model="editing.timezone" filterable placeholder="选择时区（IANA）" style="width: 100%">
            <el-option v-for="zone in timezoneOptions" :key="zone" :label="zone" :value="zone" />
          </el-select>
        </el-form-item>
        <el-form-item label="法律主体">
          <el-input v-model="editing.legalEntityName" placeholder="如 XX 株式会社" />
        </el-form-item>
        <el-form-item label="语言候选">
          <el-checkbox-group v-model="editing.locales">
            <el-checkbox v-for="locale in localeOptions" :key="locale" :value="locale" :label="locale" />
          </el-checkbox-group>
          <span class="field-hint">候选来自后台已启用/需要翻译的语言；这里不勾就不在该市场展示</span>
        </el-form-item>
        <el-form-item label="主语言">
          <el-radio-group v-model="editing.primaryLocale" :disabled="editing.locales.length === 0">
            <el-radio v-for="locale in editing.locales" :key="locale" :value="locale" :label="locale" />
          </el-radio-group>
          <span v-if="editing.locales.length === 0" class="field-hint">先勾选至少一个语言候选</span>
        </el-form-item>
        <el-form-item label="ICP 备案">
          <el-input v-model="editing.icpNumber" placeholder="该市场的备案号" />
        </el-form-item>
        <el-form-item label="客服电话">
          <el-input v-model="editing.servicePhone" placeholder="该市场的客服电话" />
        </el-form-item>
        <el-form-item label=" ">
          <span class="field-hint">
            这两项覆盖「系统设置」里的全局值：商城按访客选择的地区（页脚选择器，请求带 X-Market）取本市场值，
            留空则回退全局。同一能力只启用一个服务商。
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
              <span class="cap">{{ capabilityLabel(row.capability) }}</span>
              <span class="prov">{{ providerLabel(row.capability, row.provider) }}</span>
              <el-switch
                :model-value="row.enabled"
                :loading="integrationSaving"
                @change="(value: string | number | boolean) => toggleIntegration(row, Boolean(value))"
              />
            </div>
            <div class="integration-row">
              <el-select v-model="newIntegration.capability" style="width: 150px">
                <el-option v-for="cap in capabilities" :key="cap"
                           :label="capabilityLabel(cap)" :value="cap" />
              </el-select>
              <el-select v-model="newIntegration.provider" placeholder="选择服务商" style="flex: 1">
                <el-option v-for="prov in providersForCapability" :key="prov"
                           :label="providerLabel(newIntegration.capability, prov)" :value="prov" />
              </el-select>
              <el-button
                :loading="integrationSaving"
                :disabled="!newIntegration.provider"
                @click="addIntegration"
              >添加并启用</el-button>
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
.mb-12 { margin-bottom: 12px; }
</style>
