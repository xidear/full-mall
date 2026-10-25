<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()

interface Uploaded { url: string; key: string; bytes: number; provider: string; purpose?: string; assetId?: string }
interface AssetRow { id: string; name: string; url: string; kind: string; purpose?: string }

const uploaded = ref<Uploaded[]>([])
const assets = ref<AssetRow[]>([])
const aiJobs = ref<Array<{ id: string; kind: string; purpose: string; status: string; createdAt: string }>>([])
const purposeFilter = ref('')
const loading = ref(false)
const variantDialog = ref(false)
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

async function handleFile(file: File) {
  uploading.value = true
  try {
    const form = new FormData()
    form.append('file', file)
    const purpose = purposeValue.value
    const response = await fetch(
      `${import.meta.env.VITE_API_BASE || 'http://localhost:8080'}/admin/v1/media?purpose=${purpose}`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${localStorage.getItem('fullmall-admin-token') ?? ''}` },
        body: form
      })
    if (!response.ok) {
      const detail = await response.text()
      throw new Error(`上传失败 ${response.status}：${detail}`)
    }
    const result = await response.json() as Uploaded
    uploaded.value.unshift(result)
    ElMessage.success('已上传')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

async function loadAssets() {
  loading.value = true
  try {
    const page = await api.get<{ content: AssetRow[] }>('/admin/v1/assets?size=50&sort=createdAt,desc')
    assets.value = page.content ?? []
    aiJobs.value = await api.get('/admin/v1/ai/media/jobs?limit=10')
  } finally {
    loading.value = false
  }
}

/** 按用途过滤：OTHER_SYSTEM（审批附件等）本来就不入库，这里再做一层防御展示。 */
function visibleAssets() {
  return assets.value.filter(asset => {
    const purpose = asset.purpose ?? 'PRODUCT'
    if (purpose === 'OTHER_SYSTEM') return false
    return !purposeFilter.value || purpose === purposeFilter.value
  })
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

loadAssets()
</script>

<template>
  <div>
    <h1>素材库</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="只允许图片（png/jpeg/webp/gif），单文件上限由后端配置（默认 5MB）"
              description="存储走七牛/COS/本地，具体取决于后台「第三方服务」里的存储配置。" />

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

    <div class="mb-16">
      <label class="upload-button">
        <input type="file" accept="image/png,image/jpeg,image/webp,image/gif"
               :disabled="uploading" @change="onChange" />
        <span>{{ uploading ? '上传中…' : '选择图片上传' }}</span>
      </label>
    </div>

    <el-table :data="visibleAssets()" v-loading="loading" empty-text="素材库还没有素材">
      <el-table-column label="预览" width="110">
        <template #default="{ row }">
          <img :src="row.url" alt="素材" class="thumb" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column label="用途" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.purpose ?? 'PRODUCT' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="kind" label="类型" width="100" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="copy(row.url)">复制链接</el-button>
          <el-button link type="primary" @click="openVariant(row)">用它生成变体</el-button>
        </template>
      </el-table-column>
    </el-table>

    <h3>AI 生成记录</h3>
    <el-table :data="aiJobs" empty-text="还没有 AI 生成任务">
      <el-table-column prop="kind" label="类型" width="100" />
      <el-table-column prop="purpose" label="用途" width="110" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="createdAt" label="时间" min-width="180">
        <template #default="{ row }">{{ new Date(row.createdAt).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default>
          <el-button link type="primary" @click="$router.push('/ai-media')">去 AI 素材生成</el-button>
        </template>
      </el-table-column>
    </el-table>

    <h3>本次上传</h3>
    <el-table :data="uploaded" empty-text="还没有上传记录">
      <el-table-column label="预览" width="120">
        <template #default="{ row }">
          <img :src="row.url" alt="素材" class="thumb" />
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
            <img v-for="asset in assets.filter(a => (a.purpose ?? 'PRODUCT') !== 'OTHER_SYSTEM').slice(0, 18)"
                 :key="asset.id" :src="asset.url" class="thumb"
                 :class="{ picked: variantRefs.includes(asset.id) }"
                 :alt="asset.name" loading="lazy" @click="toggleVariantRef(asset.id)" />
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
              {{ variantJob.status }}
            </el-tag>
            <img v-if="variantJob.assetId" class="result"
                 :src="assets.find(a => a.id === variantJob?.assetId)?.url ?? ''" alt="生成结果" />
          </div>

          <template v-if="variantJob?.status === 'SUCCEEDED'">
            <h4>4. 挂到商品</h4>
            <el-input v-model="attachProductId" placeholder="商品ID" />
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
.muted { color: #999; font-size: 12px; }
.upload-button {
  display: inline-block; padding: 8px 16px; border-radius: 6px;
  background: #409eff; color: #fff; cursor: pointer;
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
