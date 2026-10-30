<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { useEnums } from '../composables/useEnums'

const { enumLabel } = useEnums()
import { apiErrorMessage } from '../composables/useAsyncState'
import AiGeneratePanel from '../components/AiGeneratePanel.vue'
import AssetPicker from '../components/AssetPicker.vue'
import ProductPicker from '../components/ProductPicker.vue'
import MediaPreview from '../components/MediaPreview.vue'
import { aiJobStatusLabels, labelOf } from '../labels'
import { acceptAttribute, loadUploadLimits, megabytes, uploadMedia, type UploadLimits } from '../upload'

const router = useRouter()

interface Uploaded {
  url: string; key: string; bytes: number; provider: string
  purpose?: string; assetId?: string | null; kind?: 'IMAGE' | 'VIDEO'
}
interface AssetRow { id: string; name: string; url: string; kind: string; purpose?: string }

const uploaded = ref<Uploaded[]>([])
const assets = ref<AssetRow[]>([])
const aiJobs = ref<Array<{ id: string; kind: string; purpose: string; status: string; createdAt: string }>>([])
const purposeFilter = ref('')
// 服务端分页 + 标签 + 关键词（与素材选择器共用同一套接口；软删除的素材不会再出现在列表里）
const keyword = ref('')
const activeTag = ref('')
const assetTags = ref<Array<{ tag: string; count: number }>>([])
const page = ref(0)
const pageSize = ref(20)
const total = ref(0)
// 搜索模式：关键词（DB 模糊）/ 描述语义（文本向量）/ 图片内容（CLIP 图像向量）
const searchMode = ref<'keyword' | 'semantic' | 'image'>('keyword')
const imageSearchAvailable = ref(false)
const scores = ref<Record<string, number>>({})
// 语言变体：同一素材在不同语言下用不同图片
const variantDialog = ref(false)
const variantAsset = ref<AssetRow | null>(null)
const variantLocales = ref<string[]>([])
const variantMap = ref<Record<string, string>>({})
const variantSaving = ref(false)

async function loadSearchCapabilities() {
  try {
    const status = await api.get<{ available: boolean }>('/admin/v1/assets/image-search/status')
    imageSearchAvailable.value = status.available
  } catch (cause) {
    imageSearchAvailable.value = false
  }
}

async function openVariants(row: AssetRow) {
  variantAsset.value = row
  variantDialog.value = true
  variantMap.value = {}
  try {
    // 接口返回 {defaultLocale, enabledLocales, translationLocales}：
    // 只有"需要翻译的语言"才有变体意义（默认语言就是原图）
    const locales = await api.get<{ translationLocales?: string[] }>('/admin/v1/i18n/locales')
    variantLocales.value = locales.translationLocales ?? []
    const existing = await api.get<Array<{ locale: string; url: string }>>(
      '/admin/v1/assets/' + row.id + '/variants')
    variantMap.value = Object.fromEntries(existing.map(item => [item.locale, item.url]))
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, '语言变体加载失败'))
  }
}

async function saveVariants() {
  if (!variantAsset.value) return
  variantSaving.value = true
  try {
    for (const [locale, url] of Object.entries(variantMap.value)) {
      if (!url) continue
      await api.put('/admin/v1/assets/' + variantAsset.value.id + '/variants/' + locale, { url })
    }
    ElMessage.success('语言变体已保存')
    variantDialog.value = false
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, '保存语言变体失败'))
  } finally {
    variantSaving.value = false
  }
}
const loading = ref(false)
// 上传区的「AI 生成」：图片/视频共用同一面板，按已配置模型决定显示哪种
const aiDialog = ref(false)
// 视觉模型是否可用（未配置时按钮禁用并提示；不影响其它功能）
const visionConfigured = ref(false)
const visionHint = ref('')
const describingId = ref('')

async function loadVisionStatus() {
  try {
    const status = await api.get<{ configured: boolean; hint?: string }>('/admin/v1/ai/providers/vision')
    visionConfigured.value = status.configured
    visionHint.value = status.hint ?? ''
  } catch (cause) {
    visionConfigured.value = false
    visionHint.value = apiErrorMessage(cause, '视觉模型状态读取失败')
  }
}

async function describeAsset(row: { id: string; name: string }) {
  describingId.value = row.id
  try {
    const result = await api.post<{ asset: { caption?: string | null; tags?: string[] }; parsed: boolean }>(
      '/admin/v1/assets/' + row.id + '/describe', {})
    ElMessage.success(result.parsed
      ? '已识别：' + (result.asset.caption ?? '').slice(0, 24) + '…'
      : '已识别（模型未按 JSON 输出，已整段作为描述）')
    await loadAssets()
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, '图片识别失败'))
  } finally {
    describingId.value = ''
  }
}

async function onGenerated() {
  await loadAssets()
loadUploadLimits()
  .then(value => { limits.value = value })
  .catch(cause => { limitsError.value = apiErrorMessage(cause, '读取上传规则失败') })
}
const variantRefs = ref<string[]>([])
const variantProviders = ref<Array<{ code: string; name: string; model: string }>>([])
const variantForm = ref({ providerCode: '', purpose: 'MAIN', prompt: '', negative: '' })
const variantJob = ref<{ id: string; status: string; assetId?: string | null; error?: string | null } | null>(null)
const variantBusy = ref(false)
const variantError = ref('')
const variantPrompts = ref<Array<{ prompt: string; negative: string }>>([])
const attachProductId = ref('')
const attachRole = ref('MAIN')
const attachDone = ref(false)

async function openVariant(asset?: AssetRow) {
  variantRefs.value = asset ? [asset.id] : []
  variantJob.value = null
  attachDone.value = false
  variantError.value = ''
  variantForm.value.prompt = ''
  variantForm.value.negative = ''
  variantDialog.value = true
  try {
    const all = await api.get<Array<{ code: string; name: string; model: string; kind: string }>>(
      '/admin/v1/ai/media/providers')
    variantProviders.value = all.filter(p => p.kind === 'IMAGE')
    variantForm.value.providerCode = variantProviders.value[0]?.code ?? ''
    variantPrompts.value = await api.get<Array<{ prompt: string; negative: string }>>(
      '/admin/v1/ai/media/prompts?limit=10')
  } catch (cause) {
    variantError.value = cause instanceof Error ? cause.message : '加载供应商失败'
  }
}

function toggleVariantRef(id: string) {
  const index = variantRefs.value.indexOf(id)
  if (index >= 0) variantRefs.value.splice(index, 1)
  else variantRefs.value.push(id)
}

async function submitVariant() {
  variantError.value = ''
  if (!variantRefs.value.length) { variantError.value = '先选择至少一张参考图'; return }
  if (!variantForm.value.prompt.trim()) { variantError.value = '提示词不能为空'; return }
  if (!variantForm.value.providerCode) { variantError.value = '未配置图片生成供应商'; return }
  variantBusy.value = true
  try {
    const created = await api.post<{ jobId: string }>('/admin/v1/ai/media/jobs', {
      kind: 'IMAGE',
      providerCode: variantForm.value.providerCode,
      purpose: variantForm.value.purpose,
      prompt: variantForm.value.prompt,
      negative: variantForm.value.negative || null,
      params: { size: '1024x1024' },
      sourceAssetIds: [],
      images: variantRefs.value.map(id => ({ assetId: id, role: 'REFERENCE' }))
    })
    variantJob.value = { id: created.jobId, status: 'PENDING' }
    await pollVariant(created.jobId)
  } catch (cause) {
    variantError.value = cause instanceof Error ? cause.message : '提交失败'
  } finally {
    variantBusy.value = false
  }
}

async function pollVariant(jobId: string) {
  for (let i = 0; i < 40; i++) {
    const job = await api.get<{ id: string; status: string; assetId?: string | null; error?: string | null }>(
      `/admin/v1/ai/media/jobs/${jobId}`)
    variantJob.value = job
    if (job.status === 'SUCCEEDED') { await loadAssets(); return }
    if (job.status === 'FAILED') { variantError.value = job.error || '生成失败'; return }
    await new Promise(resolve => setTimeout(resolve, 3000))
  }
  variantError.value = '仍在生成中，稍后到素材库查看'
}

async function attachVariant() {
  if (!variantJob.value?.assetId) { variantError.value = '还没有生成结果'; return }
  if (!attachProductId.value.trim()) { variantError.value = '填商品ID 才能挂载'; return }
  variantBusy.value = true
  try {
    await api.post(`/admin/v1/products/${attachProductId.value.trim()}/media`, {
      assetId: variantJob.value.assetId,
      role: attachRole.value,
      skuId: null,
      sortOrder: 0,
      source: 'AI'
    })
    attachDone.value = true
  } catch (cause) {
    variantError.value = cause instanceof Error ? cause.message : '挂载失败'
  } finally {
    variantBusy.value = false
  }
}
const purposeValue = ref<'PRODUCT' | 'SPEC' | 'VIDEO'>('PRODUCT')
const uploading = ref(false)
const limits = ref<UploadLimits | null>(null)
const limitsError = ref('')

// accept 与"最大 xxMB"都来自后端配置：上限在系统设置里改完，这里刷新即生效，不再写死 5MB。
const accept = computed(() => limits.value ? acceptAttribute(limits.value) : '')
const uploadHint = computed(() => {
  if (limitsError.value) return limitsError.value
  if (!limits.value) return '正在读取上传规则…'
  const types = [...limits.value.imageTypes, ...limits.value.videoTypes]
    .map(type => type.split('/')[1]).join('/')
  return `支持 ${types}；图片最大 ${megabytes(limits.value.imageMaxBytes)}MB，视频最大 ${megabytes(limits.value.videoMaxBytes)}MB（可在系统设置里调整）`
})

async function handleFile(file: File) {
  uploading.value = true
  try {
    const result = await uploadMedia(file, purposeValue.value)
    uploaded.value.unshift(result)
    ElMessage.success(result.kind === 'VIDEO' ? '视频已上传' : '图片已上传')
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, '上传失败'))
  } finally {
    uploading.value = false
  }
}

// 旧实现走 BaseCrud 的 /admin/v1/assets：不过滤软删除，也不支持标签/关键词，
// 结果列表里混进已删除素材（文件已删 → 缩略图 404）。改用检索接口。
async function loadAssets() {
  void loadVisionStatus()
  void loadSearchCapabilities()
  loading.value = true
  try {
    if (searchMode.value !== 'keyword' && keyword.value.trim()) {
      // 语义/图像检索走各自端点：返回的是"相似度排序的结果集"，不分页
      const path = searchMode.value === 'image' ? '/admin/v1/assets/image-search' : '/admin/v1/assets/semantic'
      const hits = await api.get<Array<{ asset: AssetRow; score: number }>>(path, {
        q: keyword.value.trim(),
        limit: pageSize.value
      })
      assets.value = hits.map(hit => hit.asset)
      scores.value = Object.fromEntries(hits.map(hit => [hit.asset.id, hit.score]))
      total.value = hits.length
      assetTags.value = await api.get<Array<{ tag: string; count: number }>>('/admin/v1/assets/tags')
      aiJobs.value = await api.get('/admin/v1/ai/media/jobs?limit=10')
      return
    }
    scores.value = {}
    const result = await api.get<{ items: AssetRow[]; total: number }>('/admin/v1/assets/search', {
      keyword: keyword.value.trim() || undefined,
      tag: activeTag.value || undefined,
      purpose: purposeFilter.value || undefined,
      page: page.value,
      size: pageSize.value
    })
    assets.value = (result.items ?? []).filter(asset => (asset.purpose ?? 'PRODUCT') !== 'OTHER_SYSTEM')
    total.value = result.total ?? 0
    assetTags.value = await api.get<Array<{ tag: string; count: number }>>('/admin/v1/assets/tags')
    aiJobs.value = await api.get('/admin/v1/ai/media/jobs?limit=10')
  } finally {
    loading.value = false
  }
}

function changePage(next: number) {
  page.value = next - 1
  void loadAssets()
}

function toggleTag(tag: string) {
  activeTag.value = activeTag.value === tag ? '' : tag
  page.value = 0
  void loadAssets()
}

let keywordTimer: ReturnType<typeof setTimeout> | undefined
function onKeywordInput() {
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => { page.value = 0; void loadAssets() }, 350)
}

function variantFrom(asset: AssetRow) {
  router.push({ path: '/ai-media', query: { referenceAssetId: asset.id } })
}

function onChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    void handleFile(file)
    input.value = ''
  }
}

async function copy(url: string) {
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('浏览器不允许自动复制，请手动选择')
  }
}

// 在 setup 顶层直接发起加载（不是 onMounted），且没有 catch：请求失败只有控制台 unhandled rejection，
// 页面既不显示错误也没有重试入口，表现为“素材库永远是空的”。
loadAssets()
</script>

<template>
  <div>
    <h1>素材库</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              :title="uploadHint"
              description="支持 iPhone 的 HEIC（网页端自动转成 JPEG）；SVG 不支持，请导出为 png/jpeg。存储走七牛/COS/本地，取决于「第三方服务」配置。" />

    <div class="mb-16 toolbar">
      <span class="label">用途</span>
      <el-select v-model="purposeFilter" clearable placeholder="全部用途" style="width: 160px">
        <el-option label="商品图" value="PRODUCT" />
        <el-option label="规格图" value="SPEC" />
        <el-option label="视频/封面" value="VIDEO" />
        <el-option label="AI 生成" value="AI_GENERATED" />
      </el-select>
      <el-select v-model="purposeValue" style="width: 180px">
        <el-option label="商品图" value="PRODUCT" />
        <el-option label="规格图" value="SPEC" />
        <el-option label="视频/封面" value="VIDEO" />
      </el-select>
      <el-button type="primary" plain @click="openVariant()">AI 生成变体</el-button>
      <el-button text @click="$router.push('/ai-media')">高级模式</el-button>
      <span class="muted">审批附件等非商品素材不入库</span>
    </div>

    <div class="mb-16 upload-row">
      <label class="upload-button">
        <input type="file" :accept="accept" :disabled="uploading" @change="onChange" />
        <span>{{ uploading ? '上传中…' : '选择图片/视频上传' }}</span>
      </label>
      <el-button type="primary" @click="aiDialog = true">AI 生成</el-button>
      <span class="muted">按已配置的图片/视频模型生成；提示词可复用最近使用过的</span>
    </div>

    <el-dialog v-model="aiDialog" title="AI 生成素材" width="720px" append-to-body>
      <AiGeneratePanel @generated="onGenerated" />
    </el-dialog>

    <div class="search-row">
      <el-input v-model="keyword" placeholder="名称 / 描述 / 标签；语义与图片检索可直接写一句话" clearable
                style="width: 320px" @input="onKeywordInput" />
      <el-radio-group v-model="searchMode" @change="loadAssets">
        <el-radio-button value="keyword">关键词</el-radio-button>
        <el-radio-button value="semantic">描述语义</el-radio-button>
        <el-tooltip :disabled="imageSearchAvailable" content="未配置多模态向量服务（ai_provider protocol=clip-http）">
          <span>
            <el-radio-button value="image" :disabled="!imageSearchAvailable">图片内容</el-radio-button>
          </span>
        </el-tooltip>
      </el-radio-group>
    </div>
    <div v-if="assetTags.length" class="tag-row">
      <span class="muted">标签：</span>
      <el-tag v-for="item in assetTags.slice(0, 12)" :key="item.tag" class="tag-chip"
              :type="activeTag === item.tag ? 'primary' : 'info'" @click="toggleTag(item.tag)">
        {{ item.tag }}（{{ item.count }}）
      </el-tag>
    </div>

    <el-table :data="assets" v-loading="loading" empty-text="素材库还没有匹配的素材">
      <el-table-column label="预览" width="110">
        <template #default="{ row }">
          <MediaPreview :src="row.url" alt="素材" class="thumb" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column label="用途" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ enumLabel('ASSET_PURPOSE', row.purpose ?? 'PRODUCT') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ enumLabel('ASSET_KIND', row.kind) }}</template>
      </el-table-column>
      <el-table-column label="相似度" width="90">
        <template #default="{ row }">
          <span v-if="scores[row.id]">{{ scores[row.id] }}</span>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="描述 / 标签" min-width="220">
        <template #default="{ row }">
          <div class="asset-caption">{{ row.caption || '—' }}</div>
          <div class="asset-tags">
            <el-tag v-for="tag in (row.tags ?? [])" :key="tag" size="small">{{ tag }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button link type="primary" @click="copy(row.url)">复制链接</el-button>
          <el-tooltip :disabled="visionConfigured" :content="visionHint || '未配置视觉模型'">
            <span>
              <el-button link type="primary" :disabled="!visionConfigured"
                         :loading="describingId === row.id" @click="describeAsset(row)">AI 识别</el-button>
            </span>
          </el-tooltip>
          <el-button link type="primary" @click="openVariants(row)">语言变体</el-button>
          <el-button link type="primary" @click="openVariant(row)">用它生成变体</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="variantDialog" title="素材语言变体" width="640px" append-to-body>
      <p class="muted">
        同一张素材在不同语言下改用另一张图（例如中文主图带中文文案）。商品详情渲染时会按语言自动替换，
        不需要为每种语言复制商品详情。
      </p>
      <div v-for="locale in variantLocales" :key="locale" class="variant-row">
        <span class="variant-locale">{{ locale }}</span>
        <AssetPicker v-model="variantMap[locale]" value-type="url" :label="'选择 ' + locale + ' 图片'" />
      </div>
      <el-empty v-if="!variantLocales.length" description="还没有启用其它语言（设置 → 启用语言）" />
      <template #footer>
        <el-button @click="variantDialog = false">取消</el-button>
        <el-button type="primary" :loading="variantSaving" @click="saveVariants">保存</el-button>
      </template>
    </el-dialog>

    <h3>AI 生成记录</h3>
    <el-table :data="aiJobs" empty-text="还没有 AI 生成任务">
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ enumLabel('ASSET_KIND', row.kind) }}</template>
      </el-table-column>
      <el-table-column label="用途" width="110">
        <template #default="{ row }">{{ enumLabel('ASSET_PURPOSE', row.purpose) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">{{ enumLabel('AI_JOB_STATUS', row.status) }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" min-width="180">
        <template #default="{ row }">{{ new Date(row.createdAt).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default>
          <el-button link type="primary" @click="$router.push('/ai-media')">去 AI 素材生成</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-footer">
      <span class="muted">共 {{ total }} 张素材</span>
      <el-pagination layout="prev, pager, next" :page-size="pageSize" :total="total"
                     :current-page="page + 1" @current-change="changePage" />
    </div>

    <h3>本次上传</h3>
    <el-table :data="uploaded" empty-text="还没有上传记录">
      <el-table-column label="预览" width="120">
        <template #default="{ row }">
          <MediaPreview :src="row.url" alt="素材" class="thumb" />
        </template>
      </el-table-column>
      <el-table-column prop="url" label="地址" min-width="320" />
      <el-table-column prop="provider" label="存储" width="110" />
      <el-table-column label="大小" width="110">
        <template #default="{ row }">{{ (row.bytes / 1024).toFixed(1) }} KB</template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <el-button link type="primary" @click="copy(row.url)">复制链接</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="variantDialog" title="AI 生成变体" width="760px" class="variant-dialog">
      <div class="variant-grid">
        <section>
          <h4>1. 参考图（可多张，多角度）</h4>
          <div class="thumbs">
            <MediaPreview v-for="asset in assets.filter(a => (a.purpose ?? 'PRODUCT') !== 'OTHER_SYSTEM').slice(0, 18)"
                        :key="asset.id" :src="asset.url" class="thumb"
                        :class="{ picked: variantRefs.includes(asset.id) }"
                        :alt="asset.name" @click="toggleVariantRef(asset.id)" />
          </div>
          <p class="muted">已选 {{ variantRefs.length }} 张；点击切换选择。</p>

          <h4>2. 提示词</h4>
          <el-input v-model="variantForm.prompt" type="textarea" :rows="3"
                    placeholder="主体 + 构图 + 光线 + 背景（可参考下方历史）" />
          <el-input v-model="variantForm.negative" style="margin-top: 8px"
                    placeholder="负向：logo、水印、文字" />
          <div class="history">
            <button v-for="(row, index) in variantPrompts.slice(0, 5)" :key="index" type="button"
                    class="history-item" @click="variantForm.prompt = row.prompt; variantForm.negative = row.negative">
              {{ row.prompt.slice(0, 40) }}
            </button>
          </div>
        </section>

        <section>
          <h4>3. 供应商与用途</h4>
          <el-select v-model="variantForm.providerCode" style="width: 100%"
                     :placeholder="variantProviders.length ? '选择图片供应商' : '未配置图片供应商'">
            <el-option v-for="p in variantProviders" :key="p.code"
                       :label="`${p.name}（${p.model}）`" :value="p.code" />
          </el-select>
          <el-select v-model="variantForm.purpose" style="width: 100%; margin-top: 8px">
            <el-option label="商品主图" value="MAIN" />
            <el-option label="图集" value="GALLERY" />
            <el-option label="详情图" value="DETAIL" />
          </el-select>
          <el-button type="primary" style="margin-top: 12px; width: 100%"
                     :loading="variantBusy" @click="submitVariant">生成</el-button>

          <p v-if="variantError" class="error" role="alert">{{ variantError }}</p>

          <div v-if="variantJob" class="job-state">
            <el-tag :type="variantJob.status === 'SUCCEEDED' ? 'success'
                      : variantJob.status === 'FAILED' ? 'danger' : 'warning'">
              {{ labelOf(aiJobStatusLabels, variantJob.status) }}
            </el-tag>
            <MediaPreview v-if="variantJob.assetId" class="result"
                        :src="assets.find(a => a.id === variantJob?.assetId)?.url ?? ''" alt="生成结果"
                        placeholder="生成结果待刷新" />
          </div>

          <template v-if="variantJob?.status === 'SUCCEEDED'">
            <h4>4. 挂到商品</h4>
            <!-- 挂载需要选商品：ProductPicker 必须在 script 里 import，否则组件不渲染、attachProductId 永远为空 -->
            <ProductPicker v-model="attachProductId" width="100%" />
            <el-select v-model="attachRole" style="width: 100%; margin-top: 8px">
              <el-option label="主图" value="MAIN" />
              <el-option label="图集" value="GALLERY" />
              <el-option label="详情图" value="DETAIL" />
            </el-select>
            <el-button style="margin-top: 8px" :loading="variantBusy" @click="attachVariant">挂载</el-button>
            <p v-if="attachDone" class="ok" role="status">已挂到商品</p>
          </template>
        </section>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.toolbar { display: flex; align-items: center; gap: 12px; }
.label { color: #666; }

.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.variant-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.variant-locale {
  width: 64px;
  font-size: 13px;
  color: #4a4f55;
}

.tag-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.tag-chip {
  cursor: pointer;
}

.asset-caption {
  font-size: 12px;
  color: #4a4f55;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.upload-button {
  display: inline-block; padding: 8px 16px; border-radius: 6px;
  /* #409eff 上放白字只有 2.8:1，加深到 4.6:1 */
  background: #1a66b3; color: #fff; cursor: pointer;
}
.upload-button input { display: none; }
.thumb { width: 72px; height: 72px; object-fit: cover; border-radius: 4px; }

.variant-grid { display: grid; grid-template-columns: 1.15fr 1fr; gap: 20px; max-height: 62vh; overflow-y: auto; }
@media (max-width: 768px) { .variant-grid { grid-template-columns: 1fr; max-height: 58vh; } }
.variant-grid h4 { margin: 4px 0 8px; font-size: 13px; color: #475569; }
.history { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.history-item { font-size: 12px; padding: 4px 8px; border-radius: 999px;
  border: 1px solid #e2e8f0; background: #f8fafc; cursor: pointer; color: #475569; }
.history-item:hover { border-color: #94a3b8; }
.job-state { display: flex; align-items: center; gap: 10px; margin-top: 12px; }
.result { width: 96px; height: 96px; object-fit: cover; border-radius: 6px; }
.error { color: #b91c1c; font-size: 12px; }
.ok { color: #15803d; font-size: 12px; }
</style>
