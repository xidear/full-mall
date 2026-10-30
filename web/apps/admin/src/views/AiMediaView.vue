<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import ProductPicker from '../components/ProductPicker.vue'
import SkuPicker from '../components/SkuPicker.vue'
import AssetPicker from '../components/AssetPicker.vue'
import AssetImage from '../components/AssetImage.vue'
import { aiJobStatusLabels, labelOf } from '../labels'
import AiStoryboardView from './AiStoryboardView.vue'

interface Provider { code: string; kind: string; name: string; protocol: string; model: string }
interface PromptRow { prompt: string; negative: string; providerCode: string; createdAt: string; purpose: string }
interface AssetRow { id: string; name: string; url: string; kind: string }
interface Job { id: string; status: string; assetId?: string | null; error?: string | null; prompt: string }

const route = useRoute()
const router = useRouter()

/**
 * 「视频分镜」原本是独立菜单项，但它产出的也是素材（按镜头生成视频后自动回填素材库），
 * 与本发明是同一条流水线，因此并入本页第二个标签。
 * 标签状态写回 `?tab=`：刷新/收藏/旧链接（/ai-storyboard 重定向）都能落回同一个标签。
 */
const activeTab = ref(route.query.tab === 'storyboard' ? 'storyboard' : 'generate')
watch(activeTab, tab => {
  const query = { ...route.query }
  if (tab === 'storyboard') query.tab = 'storyboard'
  else delete query.tab
  void router.replace({ path: '/ai-media', query })
})
const providers = ref<Provider[]>([])
const prompts = ref<PromptRow[]>([])
const assets = ref<AssetRow[]>([])
const jobs = ref<Job[]>([])
const selectedProduct = ref<{ id: string; title: string; skus?: Array<{ id: string; skuCode: string }> } | null>(null)
const attachRole = ref<'MAIN' | 'GALLERY' | 'DETAIL' | 'VIDEO' | 'SPEC'>('MAIN')
const attachingJob = ref('')
const loading = ref(false)
const submitting = ref(false)
const optimizing = ref(false)

const form = ref({
  kind: 'IMAGE' as 'IMAGE' | 'VIDEO' | 'TEXT',
  purpose: 'MAIN' as 'MAIN' | 'SPEC' | 'DETAIL' | 'VIDEO' | 'COPY',
  providerCode: '',
  productId: String(route.query.productId ?? ''),
  skuId: '',
  prompt: '',
  negative: '',
  size: '1024x1024',
  firstFrameUrl: '',
  referenceIds: [] as string[],
  subjectIds: [] as string[]
})

const kindProviders = computed(() => providers.value.filter(p => p.kind === form.value.kind))
const imageAssets = computed(() => assets.value.filter(a => (a.kind || '').toUpperCase() !== 'VIDEO'))

function onProductSelect(product: { id: string; title: string; skus?: Array<{ id: string; skuCode: string }> } | null) {
  selectedProduct.value = product
  form.value.skuId = ''
}

async function load() {
  loading.value = true
  try {
    providers.value = await api.get<Provider[]>('/admin/v1/ai/media/providers')
    prompts.value = await api.get<PromptRow[]>('/admin/v1/ai/media/prompts', {
      productId: form.value.productId || undefined,
      skuId: form.value.skuId || undefined,
      limit: 20
    })
    // 用检索接口而不是 BaseCrud 列表：后者不过滤软删除，会把已删素材也画出来（缩略图 404）
    const page = await api.get<{ items: AssetRow[] }>('/admin/v1/assets/search?page=0&size=50')
    assets.value = page.items ?? []
    if (!form.value.providerCode) {
      form.value.providerCode = kindProviders.value[0]?.code ?? ''
    }
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function optimize() {
  if (!form.value.prompt.trim()) { ElMessage.error('先写提示词'); return }
  optimizing.value = true
  try {
    const result = await api.post<{ prompt: string }>('/admin/v1/ai/media/prompts/optimize', {
      prompt: form.value.prompt,
      providerCode: undefined
    })
    form.value.prompt = result.prompt
    ElMessage.success('已用模型优化提示词')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '优化失败（可能未配置文本模型）')
  } finally {
    optimizing.value = false
  }
}

function usePrompt(row: PromptRow) {
  form.value.prompt = row.prompt
  form.value.negative = row.negative
  if (row.providerCode) form.value.providerCode = row.providerCode
}

function toggle(list: string[], id: string) {
  const index = list.indexOf(id)
  if (index >= 0) list.splice(index, 1)
  else list.push(id)
}

async function submit() {
  if (!form.value.prompt.trim()) { ElMessage.error('提示词必填'); return }
  if (!form.value.providerCode) { ElMessage.error('没有可用供应商，请先在第三方服务里配置'); return }
  submitting.value = true
  try {
    const created = await api.post<{ jobId: string }>('/admin/v1/ai/media/jobs', {
      kind: form.value.kind,
      providerCode: form.value.providerCode,
      productId: form.value.productId || null,
      skuId: form.value.skuId || null,
      purpose: form.value.purpose,
      prompt: form.value.prompt,
      negative: form.value.negative || null,
      params: form.value.kind === 'VIDEO'
        ? { firstFrameUrl: form.value.firstFrameUrl || null }
        : { size: form.value.size },
      sourceAssetIds: [],
      images: [
        ...form.value.referenceIds.map(id => ({ assetId: id, role: 'REFERENCE' })),
        ...form.value.subjectIds.map(id => ({ assetId: id, role: 'SUBJECT' }))
      ]
    })
    ElMessage.success('已提交，生成是异步的')
    await poll(created.jobId)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '提交失败')
  } finally {
    submitting.value = false
  }
}

async function attach(job: Job) {
  if (!job.assetId) { ElMessage.error('任务还没有产出素材'); return }
  if (!form.value.productId) { ElMessage.error('先填商品ID'); return }
  attachingJob.value = job.id
  try {
    await api.post(`/admin/v1/products/${form.value.productId}/media`, {
      assetId: job.assetId,
      role: attachRole.value,
      skuId: attachRole.value === 'SPEC' ? (form.value.skuId || null) : null,
      sortOrder: 0,
      source: 'AI'
    })
    ElMessage.success('已挂到商品素材')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '挂载失败')
  } finally {
    attachingJob.value = ''
  }
}

// 轮询 60 次 × 3s（≈3 分钟）后放弃。任务只存在本页内存（jobs.unshift）：
// 刷新页面“最近任务”就空了——后端有 /admin/v1/ai/media/jobs 列表接口，本页没有拉取。
async function poll(jobId: string) {
  for (let i = 0; i < 60; i++) {
    const job = await api.get<Job>(`/admin/v1/ai/media/jobs/${jobId}`)
    if (job.status === 'SUCCEEDED') {
      jobs.value.unshift(job)
      ElMessage.success('生成完成，已进入素材库')
      await load()
      return
    }
    if (job.status === 'FAILED') {
      jobs.value.unshift(job)
      ElMessage.error(job.error || '生成失败')
      return
    }
    await new Promise(resolve => setTimeout(resolve, 3000))
  }
  ElMessage.warning('仍在生成中，稍后到素材库查看')
}

onMounted(async () => {
  await load()
  const preset = String(route.query.referenceAssetId ?? '')
  if (preset && assets.value.some(asset => asset.id === preset)) {
    form.value.referenceIds = [preset]
  }
})
</script>

<template>
  <div>
    <h1>AI 素材生成</h1>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="图片 / 视频 / 文案" name="generate">
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="只做内容生产：商品图 / 规格图 / 详情图 / 视频 / 文案"
              description="生成是异步的，完成后自动进入素材库；提示词、参考图/引导图都会留痕并真实传给模型。" />

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>生成参数</template>
          <el-form label-width="96px">
            <el-form-item label="类型">
              <el-radio-group v-model="form.kind" @change="form.providerCode = kindProviders[0]?.code ?? ''">
                <el-radio-button value="IMAGE">图片</el-radio-button>
                <el-radio-button value="VIDEO">视频</el-radio-button>
                <el-radio-button value="TEXT">文案</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="用途">
              <el-select v-model="form.purpose" style="width: 220px">
                <el-option label="商品主图" value="MAIN" />
                <el-option label="规格图" value="SPEC" />
                <el-option label="详情图" value="DETAIL" />
                <el-option label="主图视频" value="VIDEO" />
                <el-option label="详情文案" value="COPY" />
              </el-select>
            </el-form-item>
            <el-form-item label="供应商">
              <el-select v-model="form.providerCode" style="width: 260px"
                         :placeholder="kindProviders.length ? '选择供应商' : '未配置该类型供应商'">
                <el-option v-for="p in kindProviders" :key="p.code"
                           :label="`${p.name}（${p.model}）`" :value="p.code" />
              </el-select>
            </el-form-item>
            <el-form-item label="商品">
              <ProductPicker v-model="form.productId" @select="onProductSelect" />
            </el-form-item>
            <el-form-item label="规格">
              <SkuPicker v-model="form.skuId" :product="selectedProduct" />
            </el-form-item>
            <el-form-item label="提示词">
              <el-input v-model="form.prompt" type="textarea" :rows="4" data-testid="ai-prompt"
                        placeholder="主体 + 构图 + 光线 + 背景 + 风格；可从右侧历史直接上屏" />
            </el-form-item>
            <el-form-item label="负向词">
              <el-input v-model="form.negative" type="textarea" :rows="2" placeholder="logo,水印,文字" />
            </el-form-item>
            <el-form-item v-if="form.kind === 'IMAGE'" label="尺寸">
              <el-select v-model="form.size" style="width: 180px">
                <el-option label="1024×1024（方图）" value="1024x1024" />
                <el-option label="1280×720（横图）" value="1280x720" />
                <el-option label="720×1280（竖图/主图）" value="720x1280" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="form.kind === 'VIDEO'" label="引导图（首帧）">
              <AssetPicker v-model="form.firstFrameUrl" label="选择首帧图" value-type="url" />
            </el-form-item>
            <el-form-item label="参考图（多角度）">
              <div class="thumbs">
                <!-- 候选图只从“最近 50 个素材”里取前 24 张：更早的实拍图在这个页面选不到，
                     需要去素材库/高级模式先找到再回来。 -->
                <AssetImage v-for="asset in imageAssets.slice(0, 24)" :key="asset.id" :src="asset.url"
                            class="thumb" :class="{ picked: form.referenceIds.includes(asset.id) }"
                            :alt="asset.name" @click="toggle(form.referenceIds, asset.id)" />
                <span v-if="!imageAssets.length" class="muted">素材库还没有图片，先上传实拍图</span>
              </div>
            </el-form-item>
            <el-form-item label="主体图（角色定型）">
              <div class="thumbs">
                <AssetImage v-for="asset in imageAssets.slice(0, 24)" :key="'s-' + asset.id" :src="asset.url"
                            class="thumb" :class="{ picked: form.subjectIds.includes(asset.id) }"
                            :alt="asset.name" @click="toggle(form.subjectIds, asset.id)" />
              </div>
              <div class="muted">用于视频/图片中保持角色脸部与服装一致；会映射到 MiniMax subject_reference 等字段。</div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="submit">提交生成</el-button>
              <el-button :loading="optimizing" @click="optimize">用模型优化提示词</el-button>
              <el-button :loading="loading" @click="load">刷新</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never" class="mb-16">
          <template #header>历史提示词（点击上屏）</template>
          <div v-for="(row, index) in prompts" :key="index" class="prompt-row" @click="usePrompt(row)">
            <div class="prompt-text">{{ row.prompt }}</div>
            <div class="muted">{{ row.purpose }} · {{ row.providerCode }} · {{ new Date(row.createdAt).toLocaleString() }}</div>
          </div>
          <el-empty v-if="!prompts.length" description="还没有历史提示词" :image-size="60" />
        </el-card>

        <el-card shadow="never">
          <template #header>最近任务</template>
          <div v-for="job in jobs" :key="job.id" class="job-row">
            <el-tag :type="job.status === 'SUCCEEDED' ? 'success' : job.status === 'FAILED' ? 'danger' : 'warning'">
              {{ labelOf(aiJobStatusLabels, job.status) }}
            </el-tag>
            <span class="job-prompt">{{ job.prompt }}</span>
            <el-button v-if="job.status === 'SUCCEEDED' && job.assetId" text type="primary"
                       :loading="attachingJob === job.id" @click="attach(job)">挂到商品</el-button>
          </div>
          <div class="attach-bar">
            <!-- 这里的 ProductPicker 与左侧“生成参数”共用 form.productId / form.skuId：
                 在挂载区选商品会同时改掉待生成参数里的商品，反之亦然（单表单双用途）。 -->
            <ProductPicker v-model="form.productId" width="240px" />
            <span class="muted">挂载角色</span>
            <el-select v-model="attachRole" style="width: 120px">
              <el-option label="主图" value="MAIN" />
              <el-option label="图集" value="GALLERY" />
              <el-option label="详情图" value="DETAIL" />
              <el-option label="视频" value="VIDEO" />
              <el-option label="规格图" value="SPEC" />
            </el-select>
          </div>
          <el-empty v-if="!jobs.length" description="本页还没有提交记录" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
      </el-tab-pane>

      <!-- 分镜与单镜头生成共用供应商/引导图/素材回填，只是"多个镜头排一次"；
           embedded 让它不再重复渲染自己的 H1 与信息条 -->
      <el-tab-pane label="视频分镜" name="storyboard">
        <AiStoryboardView embedded />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.thumbs { display: flex; flex-wrap: wrap; gap: 8px; }
.thumb { width: 64px; height: 64px; object-fit: cover; border-radius: 4px; cursor: pointer; border: 2px solid transparent; }
.thumb.picked { border-color: #409eff; }
.prompt-row { padding: 8px 0; border-bottom: 1px dashed #eee; cursor: pointer; }
.prompt-row:hover { background: #fafafa; }
.prompt-text { font-size: 13px; }

.job-row { display: flex; gap: 8px; align-items: center; padding: 6px 0; }
.attach-bar { display: flex; gap: 8px; align-items: center; margin-top: 8px; }
.job-prompt { font-size: 12px; color: #666; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
