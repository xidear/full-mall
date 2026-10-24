<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface Provider { code: string; kind: string; name: string; protocol: string; model: string }
interface PromptRow { prompt: string; negative: string; providerCode: string; createdAt: string; purpose: string }
interface AssetRow { id: string; name: string; url: string; kind: string }
interface Job { id: string; status: string; assetId?: string | null; error?: string | null; prompt: string }

const route = useRoute()
const providers = ref<Provider[]>([])
const prompts = ref<PromptRow[]>([])
const assets = ref<AssetRow[]>([])
const jobs = ref<Job[]>([])
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
  sourceAssetIds: [] as string[]
})

const kindProviders = computed(() => providers.value.filter(p => p.kind === form.value.kind))
const imageAssets = computed(() => assets.value.filter(a => (a.kind || '').toUpperCase() !== 'VIDEO'))

async function load() {
  loading.value = true
  try {
    providers.value = await api.get<Provider[]>('/admin/v1/ai/media/providers')
    prompts.value = await api.get<PromptRow[]>('/admin/v1/ai/media/prompts', {
      productId: form.value.productId || undefined,
      skuId: form.value.skuId || undefined,
      limit: 20
    })
    const page = await api.get<{ content: AssetRow[] }>('/admin/v1/assets?size=50&sort=createdAt,desc')
    assets.value = page.content ?? []
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

function toggleSource(id: string) {
  const index = form.value.sourceAssetIds.indexOf(id)
  if (index >= 0) form.value.sourceAssetIds.splice(index, 1)
  else form.value.sourceAssetIds.push(id)
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
      sourceAssetIds: form.value.sourceAssetIds
    })
    ElMessage.success('已提交，生成是异步的')
    await poll(created.jobId)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '提交失败')
  } finally {
    submitting.value = false
  }
}

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

onMounted(load)
</script>

<template>
  <div>
    <h1>AI 素材生成</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="只做内容生产：商品图 / 规格图 / 详情图 / 视频 / 文案"
              description="生成是异步的，完成后自动进入素材库；提示词与导图都会留痕，可复用。" />

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
            <el-form-item label="商品ID"><el-input v-model="form.productId" placeholder="可选，用于留痕与筛选" /></el-form-item>
            <el-form-item label="规格ID"><el-input v-model="form.skuId" placeholder="规格专用图时填写" /></el-form-item>
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
            <el-form-item v-if="form.kind === 'VIDEO'" label="首帧图URL">
              <el-input v-model="form.firstFrameUrl" placeholder="用已有商品图做首帧（可选）" />
            </el-form-item>
            <el-form-item label="导图">
              <div class="thumbs">
                <img v-for="asset in imageAssets.slice(0, 24)" :key="asset.id" :src="asset.url"
                     class="thumb" :class="{ picked: form.sourceAssetIds.includes(asset.id) }"
                     @click="toggleSource(asset.id)" />
                <span v-if="!imageAssets.length" class="muted">素材库还没有图片，先上传实拍图</span>
              </div>
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
              {{ job.status }}
            </el-tag>
            <span class="job-prompt">{{ job.prompt }}</span>
          </div>
          <el-empty v-if="!jobs.length" description="本页还没有提交记录" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
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
.muted { color: #999; font-size: 12px; }
.job-row { display: flex; gap: 8px; align-items: center; padding: 6px 0; }
.job-prompt { font-size: 12px; color: #666; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
