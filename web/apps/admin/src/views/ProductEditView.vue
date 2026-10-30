<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VALIDATION, type ProductUpsertRequest } from '@fullmall/api-client'
import { api } from '../api'
import AdminThumb from '../components/AdminThumb.vue'
import AssetPicker from '../components/AssetPicker.vue'
import RichTextEditor from '../components/RichTextEditor.vue'
import MarketPriceDialog from '../components/MarketPriceDialog.vue'
import { apiErrorMessage, useAsyncState } from '../composables/useAsyncState'
import { labelOf, mediaRoleLabels } from '../labels'

/**
 * 商品编辑（商品 / 素材 / 规格 / 译文 一处完成）。
 *
 * 设计要点（2026-10-28 设计审查）：
 * 1. 只调用一个详情接口，页面只有 加载中/成功/失败 三态，不会出现"半截旧数据"；
 * 2. 译文不再是独立模块：按「系统设置 → 已启用语言」渲染语言页签，支持 AI 翻译与本地化图片；
 * 3. 规格用「规格矩阵模板」生成，模板不含价格，应用时才输入价格；
 * 4. 每个可编辑区域都有自己的"保存"，并明确提示保存了什么——不再出现"点了保存其实没保存"。
 */

const router = useRouter()
const route = useRoute()
const productId = String(route.params.id ?? '')
const editing = Boolean(route.params.id)

interface SkuRow {
  id: string
  skuCode: string
  barcode?: string | null
  price: { amount: number; currency: string }
  options?: Record<string, string>
  imageUrl?: string | null
}

interface MediaItem {
  id: string
  role: string
  skuId?: string | null
  assetId: string
  url: string
  name?: string
  kind?: string
}

interface TranslationEntry {
  locale: string
  present: boolean
  title: string | null
  description: string | null
  imageUrl: string | null
  status: string | null
}

interface ProductDetail {
  id: string
  title: string
  description?: string | null
  categoryId?: string | null
  status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
  inventoryTrackingLevel: 'VARIANT' | 'PRODUCT'
  shelfLifeManaged: boolean
  skus: SkuRow[]
  media: MediaItem[]
  translations: TranslationEntry[]
  skuMatrixTemplateId?: string | null
  defaultLocale: string
  enabledLocales: string[]
  detailHtml?: string | null
}

interface MatrixTemplate {
  id: string
  code: string
  name: string
  dimensions: Record<string, string[]>
  disabled: Array<Record<string, string>>
  combinationCount: number
  enabledCombinationCount: number
  usedByProducts: number
}

const localeLabels: Record<string, string> = {
  'zh-CN': '简体中文',
  'zh-TW': '繁體中文',
  en: 'English',
  ja: '日本語',
  ko: '한국어',
  fr: 'Français',
  de: 'Deutsch',
  es: 'Español'
}

const translationStatusLabels: Record<string, string> = {
  DRAFT: '草稿',
  MACHINE: '机器翻译',
  REVIEWED: '已审核',
  PUBLISHED: '已发布',
  OUTDATED: '源文已变更'
}

const tab = ref('basics')
// 查询参数 ?tab=translations：等详情加载完成后落到第一个译文页签
let pendingTranslationsTab = String(route.query.tab ?? '') === 'translations'
const saving = ref(false)
const error = ref('')
const saveState = ref('')
const categories = ref<Array<{ id: string; name: string }>>([])
const categoriesError = ref('')

const detail = useAsyncState<ProductDetail>(() =>
  api.get<ProductDetail>('/admin/v1/products/' + productId))

const form = reactive({
  title: '',
  description: '',
  // 富文本详情（可含图片/视频/文字）；"详情图"只是它里面的图片
  detailHtml: '',
  categoryId: '',
  status: 'DRAFT' as 'DRAFT' | 'ACTIVE',
  inventoryTrackingLevel: 'VARIANT' as 'VARIANT' | 'PRODUCT',
  shelfLifeManaged: false,
  policy: {
    batchTracking: true,
    pickingStrategy: 'FEFO' as 'FEFO' | 'FIFO' | 'NONE',
    minRemainingShelfLifeDays: 2,
    nearExpiryThresholdDays: 7,
    nearExpiryAction: 'DISCOUNT' as 'NORMAL' | 'DISCOUNT' | 'DELIST' | 'WRITE_OFF',
    autoBlockExpired: true
  }
})

const mediaList = ref<MediaItem[]>([])
const mediaForm = ref({ assetId: '', role: 'MAIN', skuId: '', sortOrder: 0 })
const hasImageProvider = ref(false)
const providerHint = ref('')

const skus = ref<SkuRow[]>([])
// 分市场价格弹窗（ADR-0013 下游）：按市场覆盖价，留空即回到基础价
const marketPriceVisible = ref(false)
const marketPriceSku = ref<SkuRow | null>(null)

function openMarketPrice(row: SkuRow) {
  marketPriceSku.value = row
  marketPriceVisible.value = true
}
const priceEdits = ref<Record<string, number>>({})
const currency = ref('CNY')
const templates = ref<MatrixTemplate[]>([])
const templatesError = ref('')
const selectedTemplateId = ref('')
const skuPrefix = ref('')
const matrixPrice = ref(0)

const translations = ref<Record<string, TranslationEntry>>({})
const translationLocales = computed(() => Object.keys(translations.value))

/** 当前正在编辑的语言：语言清单可能几十项，用选中项而不是"一种语言一个页签" */
const activeLocale = ref('')
const localeFilter = ref('')

const localeLabelOf = (locale: string) => localeLabels[locale] || locale

const visibleLocales = computed(() => {
  const keyword = localeFilter.value.trim().toLowerCase()
  const all = translationLocales.value
  if (!keyword) {
    return all
  }
  return all.filter(code => code.toLowerCase().includes(keyword)
    || localeLabelOf(code).toLowerCase().includes(keyword))
})

const publishedCount = computed(() =>
  translationLocales.value.filter(code => translationOf(code).status === 'PUBLISHED').length)
const pendingCount = computed(() =>
  translationLocales.value.filter(code => translationOf(code).status !== 'PUBLISHED').length)

function localeStatusText(locale: string): string {
  const entry = translationOf(locale)
  return entry.present ? labelOf(translationStatusLabels, entry.status) : '未录入'
}

function localeTagType(locale: string): 'success' | 'warning' | 'info' {
  const status = translationOf(locale).status
  if (status === 'PUBLISHED') return 'success'
  if (status === 'OUTDATED' || status === 'REVIEWED') return 'warning'
  return 'info'
}

// 切换商品/语言清单变化时，保证选中项有效（默认选第一个待处理的，减少点击）
watch(translationLocales, (list) => {
  if (!list.length) {
    activeLocale.value = ''
    return
  }
  if (!list.includes(activeLocale.value)) {
    activeLocale.value = list.find(code => translationOf(code).status !== 'PUBLISHED') ?? list[0]
  }
}, { immediate: true })
const savingLocale = ref('')

const skuMatrixTemplateId = ref<string | null>(null)
const boundTemplate = computed(() => templates.value.find(item => item.id === skuMatrixTemplateId.value) ?? null)

// 新建态：至少一个 SKU（后端要求 skus 非空），创建后可改用模板批量生成
const firstSku = reactive({
  skuCode: '',
  barcode: '',
  priceYuan: 0,
  currency: 'CNY',
  optionName: '规格',
  optionValue: ''
})
const optionNames = ['规格', '颜色', '容量', '口味', '尺寸']
const currencies = ['CNY', 'HKD', 'MOP', 'TWD']

// 编辑态的保存门闩：详情未加载成功时 ready 为 false，保存按钮 disabled 且 saveBasics 二次拦截，
// 否则一次“加载失败后点保存”就会用空表单把原商品覆盖掉。
const ready = computed(() => !editing || Boolean(detail.data.value))

/** 分市场价格改完后重新拉详情，让 SKU 列表与基础价保持最新。 */
async function refreshDetail() {
  const detail = await api.get<ProductDetail>(`/admin/v1/products/${route.params.id}`)
  applyDetail(detail)
}

function applyDetail(value: ProductDetail) {
  form.title = value.title
  form.description = value.description ?? ''
  form.detailHtml = value.detailHtml ?? ''
  form.categoryId = value.categoryId ?? ''
  form.status = value.status === 'ACTIVE' ? 'ACTIVE' : 'DRAFT'
  form.inventoryTrackingLevel = value.inventoryTrackingLevel
  form.shelfLifeManaged = value.shelfLifeManaged
  skus.value = value.skus ?? []
  mediaList.value = value.media ?? []
  skuMatrixTemplateId.value = value.skuMatrixTemplateId ?? null
  currency.value = value.skus?.[0]?.price?.currency ?? 'CNY'
  priceEdits.value = Object.fromEntries((value.skus ?? []).map(sku => [sku.id, sku.price.amount / 100]))
  const map: Record<string, TranslationEntry> = {}
  for (const entry of value.translations ?? []) {
    map[entry.locale] = { ...entry }
  }
  translations.value = map
  // 从商品列表「译文」入口进来时，直接定位到第一个目标语言页签
  if (pendingTranslationsTab && Object.keys(map).length > 0) {
    tab.value = 'tr:' + Object.keys(map)[0]
    pendingTranslationsTab = false
  }
}

watch(() => detail.data.value, value => { if (value) applyDetail(value) })

async function loadCategories() {
  categoriesError.value = ''
  try {
    // 后台接口：admin 的 staff token 不适用于顾客端 /api/v1（会被会员鉴权判为无效）
    categories.value = await api.get<Array<{ id: string; name: string }>>('/admin/v1/categories')
  } catch (cause) {
    categories.value = []
    categoriesError.value = apiErrorMessage(cause, '类目加载失败')
  }
}

async function loadTemplates() {
  templatesError.value = ''
  try {
    templates.value = await api.get<MatrixTemplate[]>('/admin/v1/sku-matrix-templates')
  } catch (cause) {
    templates.value = []
    templatesError.value = apiErrorMessage(cause, '规格矩阵模板加载失败')
  }
}

async function loadMediaContext() {
  try {
    const providers = await api.get<Array<{ kind: string }>>('/admin/v1/ai/media/providers')
    hasImageProvider.value = providers.some(provider => provider.kind === 'IMAGE')
    providerHint.value = hasImageProvider.value ? '' : '未配置图片生成供应商'
  } catch (cause) {
    hasImageProvider.value = false
    providerHint.value = apiErrorMessage(cause, 'AI 供应商状态读取失败')
  }
}

// 类目、模板、AI 供应商各自独立加载、各自独立错误（categoriesError / templatesError / providerHint）：
// 任一失败只降级对应区域，不阻断整页编辑。
loadCategories()
loadTemplates()
loadMediaContext()
if (editing) {
  void detail.run()
}

function openMaterials() {
  router.push('/materials')
}

function openAiMedia() {
  router.push({ path: '/ai-media', query: { productId } })
}

async function saveBasics() {
  error.value = ''
  saveState.value = ''
  if (!form.title.trim()) {
    error.value = '请填写商品标题'
    return
  }
  saving.value = true
  try {
    if (editing) {
      if (!ready.value) {
        error.value = '商品详情未加载成功，禁止保存（否则会用空表单覆盖原数据）'
        return
      }
      // 编辑态只提交标题/描述/类目三个字段：不碰 status 与 skus，避免把别人刚改的价格或上下架状态覆盖回去。
      await api.put('/admin/v1/products/' + productId, {
        title: form.title.trim(),
        description: form.description.trim() || null,
        categoryId: form.categoryId || null,
        // 服务端会用 jsoup 白名单再清洗一遍（前端可绕过，服务端才是权威）
        detailHtml: form.detailHtml
      })
      ElMessage.success('基础信息已保存')
      saveState.value = '基础信息已保存（标题、描述、类目）'
      await detail.run()
      return
    }
    const payload: ProductUpsertRequest = {
      title: form.title.trim(),
      description: form.description.trim() || undefined,
      categoryId: form.categoryId || undefined,
      status: form.status,
      inventoryTrackingLevel: form.inventoryTrackingLevel,
      shelfLifeManaged: form.shelfLifeManaged,
      skus: [{
        skuCode: firstSku.skuCode.trim(),
        barcode: firstSku.barcode.trim() || undefined,
        priceAmount: Math.round(firstSku.priceYuan * 100),
        currency: firstSku.currency,
        options: { [firstSku.optionName]: firstSku.optionValue.trim() },
        imageUrl: undefined
      }]
    }
    if (!payload.skus[0].skuCode || !firstSku.optionValue.trim()) {
      error.value = '请填写首个 SKU 的编码与规格值'
      return
    }
    if (!new RegExp(VALIDATION.skuCodePattern).test(payload.skus[0].skuCode)) {
      error.value = 'SKU 编码只能包含字母、数字、点、下划线、短横线，最长 64 位'
      return
    }
    if (firstSku.priceYuan <= 0) {
      error.value = '价格必须大于 0'
      return
    }
    const created = await api.post<ProductDetail>('/admin/v1/products', payload)
    ElMessage.success('商品已创建，可继续配置规格矩阵与译文')
    await router.replace('/products/' + created.id + '/edit')
  } catch (cause) {
    error.value = apiErrorMessage(cause, '保存失败')
  } finally {
    saving.value = false
  }
}

// 价格保存只提交“与后端当前值不同”的 SKU（比较用分，避免浮点误差），没变化直接提示；
// 这样批量改价不会把未改动的行也写一遍，减少并发覆盖面。
async function savePrices() {
  const items = skus.value
    .filter(sku => {
      const edited = priceEdits.value[sku.id]
      return edited !== undefined && Math.round(edited * 100) !== sku.price.amount
    })
    .map(sku => ({ skuId: sku.id, priceAmount: Math.round(priceEdits.value[sku.id] * 100) }))
  if (items.length === 0) {
    ElMessage.info('价格没有变化')
    return
  }
  saving.value = true
  try {
    await api.post('/admin/v1/products/skus/batch-price', { items })
    ElMessage.success('价格已保存')
    saveState.value = '已保存 ' + items.length + ' 个 SKU 的价格'
    await detail.run()
  } catch (cause) {
    error.value = apiErrorMessage(cause, '价格保存失败')
  } finally {
    saving.value = false
  }
}

async function applyTemplate() {
  error.value = ''
  if (!selectedTemplateId.value) {
    error.value = '请先选择规格矩阵模板'
    return
  }
  if (!skuPrefix.value.trim()) {
    error.value = '请填写 SKU 编码前缀（例如 PHONE）'
    return
  }
  if (matrixPrice.value <= 0) {
    error.value = '请填写应用模板时的价格（模板本身不存价格）'
    return
  }
  saving.value = true
  try {
    const result = await api.post<{ created: number; skipped: number; disabled: number }>(
      '/admin/v1/sku-matrix-templates/' + selectedTemplateId.value + '/apply',
      {
        productId,
        prefix: skuPrefix.value.trim(),
        priceAmount: Math.round(matrixPrice.value * 100),
        currency: currency.value
      })
    ElMessage.success('已生成 ' + result.created + ' 个 SKU（跳过 ' + result.skipped + '，禁用组合 ' + result.disabled + '）')
    saveState.value = '按模板生成 ' + result.created + ' 个 SKU'
    await detail.run()
  } catch (cause) {
    error.value = apiErrorMessage(cause, '应用模板失败')
  } finally {
    saving.value = false
  }
}

async function attachMedia() {
  if (!mediaForm.value.assetId) { ElMessage.error('先选择素材'); return }
  try {
    await api.post('/admin/v1/products/' + productId + '/media', {
      assetId: mediaForm.value.assetId,
      role: mediaForm.value.role,
      skuId: mediaForm.value.role === 'SPEC' ? (mediaForm.value.skuId || null) : null,
      sortOrder: mediaForm.value.sortOrder,
      source: 'UPLOAD'
    })
    mediaForm.value.assetId = ''
    await detail.run()
    ElMessage.success('已挂到商品')
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, '挂载失败'))
  }
}

async function detachMedia(id: string) {
  try {
    await api.delete('/admin/v1/products/' + productId + '/media/' + id)
    await detail.run()
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, '移除失败'))
  }
}

function translationOf(locale: string): TranslationEntry {
  return translations.value[locale] ?? {
    locale, present: false, title: null, description: null, imageUrl: null, status: null
  }
}

function updateTranslation(locale: string, patch: Partial<TranslationEntry>) {
  translations.value = {
    ...translations.value,
    [locale]: { ...translationOf(locale), ...patch }
  }
}

async function aiTranslate(locale: string) {
  const entry = translationOf(locale)
  if (entry.present && (entry.title ?? '').trim()) {
    try {
      await ElMessageBox.confirm(
        '该语言已有译文，AI 翻译会用机器译文覆盖当前草稿（已发布的译文需要重新审核发布）。继续吗？',
        '覆盖已有译文', { confirmButtonText: '覆盖', cancelButtonText: '取消', type: 'warning' })
    } catch {
      return
    }
  }
  savingLocale.value = locale
  try {
    const row = await api.post<{ title: string; description: string | null; imageUrl: string | null; status: string }>(
      '/admin/v1/products/' + productId + '/translations/' + locale + '/auto-translate', {})
    updateTranslation(locale, {
      present: true, title: row.title, description: row.description,
      imageUrl: row.imageUrl, status: row.status
    })
    ElMessage.success('已生成机器翻译草稿，请审核后发布')
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, 'AI 翻译失败'))
  } finally {
    savingLocale.value = ''
  }
}

async function saveTranslation(locale: string) {
  const entry = translationOf(locale)
  if (!(entry.title ?? '').trim()) {
    ElMessage.error('译文标题必填')
    return
  }
  savingLocale.value = locale
  try {
    const row = await api.put<{ title: string; description: string | null; imageUrl: string | null; status: string }>(
      '/admin/v1/products/' + productId + '/translations/' + locale,
      {
        title: (entry.title ?? '').trim(),
        description: (entry.description ?? '').trim() || null,
        imageUrl: (entry.imageUrl ?? '').trim() || null
      })
    updateTranslation(locale, { present: true, status: row.status })
    ElMessage.success('译文草稿已保存')
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, '译文保存失败'))
  } finally {
    savingLocale.value = ''
  }
}

// 富文本详情的翻译结果（只替换文本节点，结构与图片/视频保持）
const detailTranslation = ref<{ html: string; segments: number; cached: number } | null>(null)

async function translateDetail(locale: string) {
  savingLocale.value = locale
  detailTranslation.value = null
  try {
    const result = await api.post<{ html: string; segments: number; cached: number }>(
      '/admin/v1/products/' + productId + '/translations/' + locale + '/auto-translate-detail', {})
    detailTranslation.value = result
    ElMessage.success('详情已翻译：共 ' + result.segments + ' 段，命中翻译记忆 ' + result.cached + ' 段（未重复计费）')
    await detail.run()
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, '详情翻译失败'))
  } finally {
    savingLocale.value = ''
  }
}

async function transitionTranslation(locale: string, action: 'review' | 'publish') {
  savingLocale.value = locale
  try {
    const row = await api.post<{ status: string }>(
      '/admin/v1/products/' + productId + '/translations/' + locale + '/' + action, {})
    updateTranslation(locale, { status: row.status })
    ElMessage.success(action === 'review' ? '已审核，可以发布' : '已发布，前台立即生效')
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, action === 'review' ? '审核失败' : '发布失败'))
  } finally {
    savingLocale.value = ''
  }
}

/** 规格专用图（role=SPEC + skuId）：真相来源是 product_media，不是 sku.image_url。 */
function specMediaOf(skuId: string) {
  return mediaList.value.find(item => item.role === 'SPEC' && item.skuId === skuId) ?? null
}

function specAssetIdOf(skuId: string): string {
  return specMediaOf(skuId)?.assetId ?? ''
}

function specImageOf(row: SkuRow): string {
  return specMediaOf(row.id)?.url ?? row.imageUrl ?? ''
}

async function attachSpecImage(skuId: string, assetId: string) {
  if (!assetId) return
  try {
    await api.post('/admin/v1/products/' + productId + '/media', {
      assetId,
      role: 'SPEC',
      skuId,
      sortOrder: 0,
      source: 'UPLOAD'
    })
    await detail.run()
    ElMessage.success('规格图已更新')
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, '规格图保存失败'))
  }
}

function skuOptions(options?: Record<string, string>) {
  if (!options) return '-'
  return Object.entries(options).map(([name, value]) => name + '：' + value).join(' / ')
}
</script>

<template>
  <div class="panel">
    <div class="panel-header">
      <div>
        <h2>{{ editing ? '编辑商品' : '新建商品' }}</h2>
        <p class="muted">基础信息、素材、规格矩阵与译文都在本页完成；译文按「系统设置 → 已启用语言」展示。</p>
      </div>
      <div>
        <el-button @click="router.back()">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!ready" @click="saveBasics">
          {{ editing ? '保存基础信息' : '创建商品' }}
        </el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
    <el-alert v-if="detail.error.value" type="error" show-icon :closable="false" class="mb-16"
              :title="'商品详情加载失败：' + detail.error.value">
      <template #default>
        <span>页面处于不可保存状态，避免用空表单覆盖原数据。</span>
        <el-button link type="primary" @click="detail.run">重试</el-button>
      </template>
    </el-alert>
    <el-alert v-if="categoriesError" type="warning" :title="categoriesError" show-icon :closable="false" class="mb-16" />
    <el-alert v-if="templatesError" type="warning" :title="templatesError" show-icon :closable="false" class="mb-16" />
    <p v-if="saveState" class="muted mb-16">{{ saveState }}</p>

    <el-tabs v-model="tab">
      <el-tab-pane label="基础信息" name="basics">
        <el-form label-position="top">
          <el-form-item label="商品标题">
            <el-input v-model="form.title" maxlength="255" show-word-limit placeholder="例如：有机鲜牛奶 1L" />
          </el-form-item>
          <el-form-item label="商品描述">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="面向顾客的商品描述" />
          </el-form-item>
          <div class="policy-grid">
            <el-form-item label="类目">
              <el-select v-model="form.categoryId" clearable style="width: 100%" placeholder="选择类目">
                <el-option v-for="category in categories" :key="category.id"
                           :label="category.name" :value="category.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="上架" value="ACTIVE" />
              </el-select>
            </el-form-item>
            <el-form-item label="库存粒度">
              <el-select v-model="form.inventoryTrackingLevel" style="width: 100%">
                <el-option label="按规格（SKU）" value="VARIANT" />
                <el-option label="按商品（SPU）" value="PRODUCT" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="启用批次/效期管理">
            <el-switch v-model="form.shelfLifeManaged" />
          </el-form-item>
          <div v-if="form.shelfLifeManaged" class="policy-grid">
            <el-form-item label="强制批次">
              <el-switch v-model="form.policy.batchTracking" />
            </el-form-item>
            <el-form-item label="拣货策略">
              <el-select v-model="form.policy.pickingStrategy" style="width: 100%">
                <el-option label="FEFO：先到期先出" value="FEFO" />
                <el-option label="FIFO：先进先出" value="FIFO" />
                <el-option label="NONE：不按批次" value="NONE" />
              </el-select>
            </el-form-item>
            <el-form-item label="最低剩余保质期（天）">
              <el-input-number v-model="form.policy.minRemainingShelfLifeDays" :min="0" :max="3650" />
            </el-form-item>
            <el-form-item label="临期阈值（天）">
              <el-input-number v-model="form.policy.nearExpiryThresholdDays" :min="0" :max="3650" />
            </el-form-item>
            <el-form-item label="临期动作">
              <el-select v-model="form.policy.nearExpiryAction" style="width: 100%">
                <el-option label="正常销售" value="NORMAL" />
                <el-option label="折扣清货" value="DISCOUNT" />
                <el-option label="下架" value="DELIST" />
                <el-option label="报损" value="WRITE_OFF" />
              </el-select>
            </el-form-item>
            <el-form-item label="过期自动阻断">
              <el-switch v-model="form.policy.autoBlockExpired" />
            </el-form-item>
          </div>
          <p class="muted">基础信息保存只包含标题、描述与类目；状态、库存粒度、保质期在创建商品时写入。</p>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="素材" name="media">
        <template v-if="editing">
          <div class="media-grid">
            <div v-for="item in mediaList" :key="item.id" class="media-card">
              <AdminThumb :src="item.url" :alt="item.name || item.role" :size="150" :radius="6" />
              <div class="media-meta">
                <el-tag size="small">{{ labelOf(mediaRoleLabels, item.role) }}</el-tag>
                <el-button text type="danger" size="small" @click="detachMedia(item.id)">移除</el-button>
              </div>
            </div>
            <div class="media-card empty">
              <AssetPicker v-model="mediaForm.assetId" label="选择素材" />
              <el-select v-model="mediaForm.role" style="width: 100%; margin-top: 6px">
                <el-option label="主图" value="MAIN" />
                <el-option label="图集" value="GALLERY" />
                <el-option label="详情图" value="DETAIL" />
                <el-option label="视频" value="VIDEO" />
              </el-select>
              <el-button style="margin-top: 6px" @click="attachMedia">挂到商品</el-button>
              <el-button text type="primary" :disabled="!hasImageProvider" @click="openAiMedia">
                主图不够？AI 生成
              </el-button>
              <span v-if="providerHint" class="muted">{{ providerHint }}</span>
              <el-button text @click="openMaterials">去素材库</el-button>
            </div>
          </div>
        </template>
        <el-empty v-else description="先创建商品，再挂素材与规格矩阵" />
      </el-tab-pane>

      <el-tab-pane label="详情" name="detail">
        <template v-if="editing">
          <p class="muted">
            详情是富文本：文字、图片、视频混排；「详情图」只是其中的图片素材。
            保存基础信息时一起提交，服务端会做白名单清洗。
          </p>
          <RichTextEditor v-model="form.detailHtml" />
        </template>
        <el-empty v-else description="先创建商品，再编辑富文本详情" />
      </el-tab-pane>

      <el-tab-pane label="规格" name="skus">
        <template v-if="editing">
          <section class="form-section">
            <h3>规格矩阵模板</h3>
            <p>
              模板只保存维度、取值与禁用组合，<strong>不含价格</strong>；应用时再为这批 SKU 定价。
              也可以到「规格矩阵模板」里新建模板，供多个商品复用。
            </p>
            <div class="policy-grid">
              <el-form-item label="选择模板">
                <el-select v-model="selectedTemplateId" clearable style="width: 100%" placeholder="选择规格矩阵模板">
                  <el-option v-for="template in templates" :key="template.id"
                             :label="template.name + '（' + template.code + '，' + template.enabledCombinationCount + ' 个组合）'"
                             :value="template.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="SKU 编码前缀">
                <el-input v-model="skuPrefix" placeholder="例如 PHONE" />
              </el-form-item>
              <el-form-item label="应用模板时的价格（元）">
                <el-input-number v-model="matrixPrice" :min="0" :precision="2" :step="10" controls-position="right" />
              </el-form-item>
              <el-form-item label="币种">
                <el-select v-model="currency">
                  <el-option v-for="item in currencies" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </div>
            <p v-if="boundTemplate" class="muted">
              当前商品使用模板：{{ boundTemplate.name }}（{{ boundTemplate.code }}）
            </p>
            <el-button type="primary" :loading="saving" @click="applyTemplate">应用模板生成 SKU</el-button>
          </section>

          <section class="form-section">
            <h3>SKU（{{ skus.length }}）</h3>
            <el-table :data="skus" empty-text="还没有 SKU">
              <el-table-column label="规格图" width="150">
                <template #default="{ row }">
                  <div class="sku-image">
                    <AdminThumb :src="specImageOf(row)" :alt="row.skuCode" :size="40" />
                    <AssetPicker :model-value="specAssetIdOf(row.id)" value-type="id"
                                 label="选择规格图"
                                 @update:model-value="(value: string | string[]) => attachSpecImage(row.id, String(value))" />
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="skuCode" label="编码" min-width="160" />
              <el-table-column label="规格" min-width="180">
                <template #default="{ row }">{{ skuOptions(row.options) }}</template>
              </el-table-column>
              <el-table-column label="价格（元）" width="180">
                <template #default="{ row }">
                  <el-input-number v-model="priceEdits[row.id]" :min="0" :precision="2"
                                   controls-position="right" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="分市场价" width="110">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openMarketPrice(row)">设置</el-button>
                </template>
              </el-table-column>
              <el-table-column label="币种" width="90">
                <template #default="{ row }">{{ row.price.currency }}</template>
              </el-table-column>
            </el-table>
            <div class="table-footer">
              <span class="muted">改价后需要点右侧按钮保存（以前改完直接切页会丢）</span>
              <el-button type="primary" plain :loading="saving" @click="savePrices">保存价格</el-button>
              <MarketPriceDialog
                v-if="marketPriceSku"
                v-model="marketPriceVisible"
                :sku-id="marketPriceSku.id"
                :sku-code="marketPriceSku.skuCode"
                :base-amount="marketPriceSku.price.amount"
                :base-currency="marketPriceSku.price.currency"
                @changed="refreshDetail"
              />
            </div>
          </section>
        </template>
        <template v-else>
          <section class="form-section">
            <h3>首个 SKU</h3>
            <p>创建商品至少需要一个 SKU；创建后可用规格矩阵模板批量生成其余组合。</p>
            <div class="variant-row">
              <el-input v-model="firstSku.skuCode" :maxlength="64" placeholder="SKU 编码（字母/数字/._-）" />
              <el-input v-model="firstSku.barcode" placeholder="条码（可选）" />
              <el-input-number v-model="firstSku.priceYuan" :min="0" :precision="2" :step="1" controls-position="right" />
              <el-select v-model="firstSku.currency">
                <el-option v-for="item in currencies" :key="item" :label="item" :value="item" />
              </el-select>
              <el-select v-model="firstSku.optionName">
                <el-option v-for="name in optionNames" :key="name" :label="name" :value="name" />
              </el-select>
              <el-input v-model="firstSku.optionValue" placeholder="规格值" />
            </div>
          </section>
        </template>
      </el-tab-pane>

      <!-- 多语言只占一个 tab：语言数量可变（可能几十种），不可能一种一个顶级标签页。
           左侧是语言清单（带状态，一眼看出谁没录/谁待审核），右侧只编辑当前选中的语言。 -->
      <el-tab-pane label="多语言" name="translations">
        <div class="locale-layout">
          <aside class="locale-side">
            <p class="locale-summary">
              共 {{ translationLocales.length }} 种语言 · 已发布 {{ publishedCount }} · 待处理 {{ pendingCount }}
            </p>
            <el-input v-model="localeFilter" placeholder="搜索语言（名称或代码）" clearable size="small" />
            <ul class="locale-list">
              <li v-for="item in visibleLocales" :key="item"
                  :class="{ active: item === activeLocale }"
                  @click="activeLocale = item">
                <span class="locale-name">{{ localeLabelOf(item) }}</span>
                <el-tag size="small" :type="localeTagType(item)">{{ localeStatusText(item) }}</el-tag>
              </li>
            </ul>
          </aside>
          <section class="locale-main">
            <el-empty v-if="!activeLocale" description="没有可编辑的语言：请先在「市场配置」里添加语言候选" />
            <template v-else>
        <template v-if="editing">
          <div class="translation-head">
            <div>
              <h3>{{ localeLabelOf(activeLocale) }}（{{ activeLocale }}）</h3>
              <p class="muted">
                源语言：{{ detail.data.value?.defaultLocale || 'zh-CN' }}；
                机器翻译只写入草稿，审核后发布才会在前台生效。
              </p>
            </div>
            <el-tag :type="translationOf(activeLocale).status === 'PUBLISHED' ? 'success'
                     : translationOf(activeLocale).status === 'OUTDATED' ? 'warning' : 'info'">
              {{ translationOf(activeLocale).present
                ? labelOf(translationStatusLabels, translationOf(activeLocale).status)
                : '未录入' }}
            </el-tag>
          </div>

          <el-form label-position="top">
            <el-form-item label="译文标题">
              <el-input :model-value="translationOf(activeLocale).title ?? ''"
                        :placeholder="form.title || '与源语言一致的标题'"
                        @update:model-value="(value: string) => updateTranslation(activeLocale, { title: value })" />
            </el-form-item>
            <el-form-item label="译文描述">
              <el-input :model-value="translationOf(activeLocale).description ?? ''" type="textarea" :rows="4"
                        @update:model-value="(value: string) => updateTranslation(activeLocale, { description: value })" />
            </el-form-item>
            <el-form-item label="本地化商品图（可选）">
              <div class="translation-image">
                <AdminThumb :src="translationOf(activeLocale).imageUrl" :size="32" :radius="6" />
                <el-input :model-value="translationOf(activeLocale).imageUrl ?? ''" placeholder="https://... 或 /images/..."
                          @update:model-value="(value: string) => updateTranslation(activeLocale, { imageUrl: value })" />
                <AssetPicker :model-value="translationOf(activeLocale).imageUrl ?? ''" value-type="url"
                             label="选素材"
                             @update:model-value="(value: string | string[]) => updateTranslation(activeLocale, { imageUrl: String(value) })" />
              </div>
            </el-form-item>
          </el-form>

          <div v-if="detailTranslation" class="detail-preview">
            <h4>详情译文预览（{{ detailTranslation.segments }} 段，命中记忆 {{ detailTranslation.cached }} 段）</h4>
            <!-- 只用于预览核对；正式展示在商城商品页（v-html 由编辑器产出的受限 HTML） -->
            <div class="rich-preview" v-html="detailTranslation.html" />
          </div>

          <div class="translation-actions">
            <el-button :loading="savingLocale === activeLocale" @click="aiTranslate(activeLocale)">AI 翻译</el-button>
            <el-button :loading="savingLocale === activeLocale" @click="translateDetail(activeLocale)">翻译富文本详情</el-button>
            <el-button :loading="savingLocale === activeLocale" @click="saveTranslation(activeLocale)">保存草稿</el-button>
            <el-button :disabled="translationOf(activeLocale).status !== 'DRAFT' && translationOf(activeLocale).status !== 'MACHINE'"
                       :loading="savingLocale === activeLocale" @click="transitionTranslation(activeLocale, 'review')">审核</el-button>
            <el-button type="primary" :disabled="translationOf(activeLocale).status !== 'REVIEWED'"
                       :loading="savingLocale === activeLocale" @click="transitionTranslation(activeLocale, 'publish')">发布</el-button>
          </div>
              </template>
            </template>
          </section>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.locale-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 18px;
  align-items: start;
}

.locale-side {
  border-right: 1px solid #ece9e2;
  padding-right: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.locale-summary {
  margin: 0;
  color: #6b7280;
  font-size: 12px;
}

.locale-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 520px;
  overflow-y: auto;
}

.locale-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
}

.locale-list li:hover {
  background: #f6f4ef;
}

.locale-list li.active {
  background: #fdecec;
  font-weight: 600;
}

.locale-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mb-16 {
  margin-bottom: 16px;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.media-card {
  border: 1px solid #ece9e2;
  border-radius: 10px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.media-card.empty {
  justify-content: center;
}

.media-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.translation-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.translation-head h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.translation-image {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.detail-preview {
  margin: 12px 0 16px;
  padding: 12px;
  border: 1px dashed #dedbd3;
  border-radius: 10px;
}

.detail-preview h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #5f666d;
}

.rich-preview :deep(img),
.rich-preview :deep(video) {
  max-width: 100%;
  border-radius: 6px;
}

.sku-image {
  display: flex;
  align-items: center;
  gap: 8px;
}

.translation-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
