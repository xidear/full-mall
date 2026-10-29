<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { apiErrorMessage } from '../composables/useAsyncState'

/**
 * AI 生成面板（图片 / 视频）：素材库上传区与素材选择器共用同一份实现。
 *
 * <p>为什么抽成组件：以前只有一个"AI 生成变体"弹窗，且强制选参考图、只支持图片，
 * 视频供应商配好了也用不上。这里按"已配置的模型"决定显示哪些类型，
 * 并复用提示词历史（/admin/v1/ai/media/prompts）。
 */
// 注意：这里传的是 **AI 任务的用途**（MAIN/SPEC/DETAIL/VIDEO/COPY），
// 不是素材用途（PRODUCT/AI_GENERATED…），两者是不同枚举，混用会被数据库约束拒绝。
const props = withDefaults(defineProps<{
  productId?: string
  compact?: boolean
}>(), { compact: false })

const emit = defineEmits<{ generated: [assetId: string] }>()

interface Provider { code: string; name: string; model: string; kind: string }
interface JobView { id: string; status: string; assetId?: string | null; error?: string | null }

const providers = ref<Provider[]>([])
const providersError = ref('')
const kind = ref<'IMAGE' | 'VIDEO'>('IMAGE')
const providerCode = ref('')
const prompt = ref('')
const negative = ref('')
const size = ref('1024x1024')
const duration = ref(5)
const history = ref<Array<{ prompt: string; negative: string; providerCode: string }>>([])
const submitting = ref(false)
const error = ref('')
const job = ref<JobView | null>(null)

const kinds = computed(() => {
  const available = new Set(providers.value.map(item => item.kind))
  return (['IMAGE', 'VIDEO'] as const).filter(item => available.has(item))
})
const currentProviders = computed(() => providers.value.filter(item => item.kind === kind.value))
const providerHint = computed(() => {
  if (providersError.value) return providersError.value
  if (providers.value.length === 0) return '还没有配置生成供应商（见「第三方服务 → AI 供应商」）'
  if (kinds.value.length === 1) return '只配置了' + (kinds.value[0] === 'IMAGE' ? '图片' : '视频') + '生成供应商'
  return ''
})

onMounted(load)

async function load() {
  providersError.value = ''
  try {
    providers.value = await api.get<Provider[]>('/admin/v1/ai/media/providers')
    if (kinds.value.length > 0) {
      kind.value = kinds.value[0]
      providerCode.value = currentProviders.value[0]?.code ?? ''
    }
    history.value = await api.get<Array<{ prompt: string; negative: string; providerCode: string }>>(
      '/admin/v1/ai/media/prompts?limit=10')
  } catch (cause) {
    providersError.value = apiErrorMessage(cause, '生成供应商加载失败')
  }
}

function pickKind(next: 'IMAGE' | 'VIDEO') {
  kind.value = next
  providerCode.value = currentProviders.value[0]?.code ?? ''
}

function usePrompt(item: { prompt: string; negative: string }) {
  prompt.value = item.prompt
  negative.value = item.negative ?? ''
}

async function submit() {
  error.value = ''
  if (!providerCode.value) { error.value = '未配置' + (kind.value === 'IMAGE' ? '图片' : '视频') + '生成供应商'; return }
  if (!prompt.value.trim()) { error.value = '提示词不能为空'; return }
  submitting.value = true
  job.value = null
  try {
    const created = await api.post<{ jobId: string }>('/admin/v1/ai/media/jobs', {
      kind: kind.value,
      providerCode: providerCode.value,
      productId: props.productId || null,
      purpose: kind.value === 'VIDEO' ? 'VIDEO' : 'MAIN',
      prompt: prompt.value.trim(),
      negative: negative.value.trim() || null,
      params: kind.value === 'IMAGE' ? { size: size.value } : { duration: duration.value },
      sourceAssetIds: [],
      images: []
    })
    job.value = { id: created.jobId, status: 'PENDING' }
    await poll(created.jobId)
  } catch (cause) {
    error.value = apiErrorMessage(cause, '提交生成任务失败')
  } finally {
    submitting.value = false
  }
}

async function poll(jobId: string) {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    await new Promise(resolve => setTimeout(resolve, 2000))
    const current = await api.get<JobView>('/admin/v1/ai/media/jobs/' + jobId)
    job.value = current
    if (current.status === 'SUCCEEDED') {
      ElMessage.success('已生成并入库（可在此处直接选用）')
      if (current.assetId) emit('generated', current.assetId)
      return
    }
    if (current.status === 'FAILED') {
      error.value = current.error || '生成失败'
      return
    }
  }
  error.value = '生成超时（2 分钟内未完成），可在「AI 素材生成」页查看进度'
}
</script>

<template>
  <div class="ai-panel">
    <el-alert v-if="providerHint" :title="providerHint" type="warning" show-icon :closable="false" class="mb-12" />

    <div class="row">
      <el-radio-group v-if="kinds.length > 1" :model-value="kind" @change="(value: string | number | boolean | undefined) => pickKind(value as 'IMAGE' | 'VIDEO')">
        <el-radio-button value="IMAGE">图片</el-radio-button>
        <el-radio-button value="VIDEO">视频</el-radio-button>
      </el-radio-group>
      <el-select v-model="providerCode" placeholder="生成供应商" style="width: 220px">
        <el-option v-for="item in currentProviders" :key="item.code"
                   :label="item.name + '（' + item.model + '）'" :value="item.code" />
      </el-select>
      <el-select v-if="kind === 'IMAGE'" v-model="size" style="width: 150px">
        <el-option label="1024×1024" value="1024x1024" />
        <el-option label="1280×720" value="1280x720" />
        <el-option label="720×1280" value="720x1280" />
      </el-select>
      <el-select v-else v-model="duration" style="width: 120px">
        <el-option :label="'5 秒'" :value="5" />
        <el-option :label="'10 秒'" :value="10" />
      </el-select>
    </div>

    <el-input v-model="prompt" type="textarea" :rows="compact ? 2 : 3"
              placeholder="主体 + 构图 + 光线 + 背景（例如：白底商品主图，陶瓷马克杯，45 度侧光）" />
    <el-input v-model="negative" style="margin-top: 8px" placeholder="不要出现：文字水印、变形、多只手（可选）" />

    <div v-if="history.length" class="history">
      <span class="hint">最近的提示词：</span>
      <el-tag v-for="item in history.slice(0, 6)" :key="item.prompt" class="history-tag"
              @click="usePrompt(item)">{{ item.prompt.slice(0, 18) }}</el-tag>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mt-12" />

    <div class="row mt-12">
      <el-button type="primary" :loading="submitting" @click="submit">
        生成{{ kind === 'IMAGE' ? '图片' : '视频' }}
      </el-button>
      <el-tag v-if="job" :type="job.status === 'SUCCEEDED' ? 'success' : job.status === 'FAILED' ? 'danger' : 'warning'">
        {{ job.status === 'SUCCEEDED' ? '已完成' : job.status === 'FAILED' ? '失败' : '生成中' }}
      </el-tag>
      <span class="muted">生成结果自动入库，并带上提示词生成的标签（可被语义检索命中）</span>
    </div>
  </div>
</template>

<style scoped>
.row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.history {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.history-tag {
  cursor: pointer;
}

.hint {
  font-size: 12px;
  color: #5f666d;
}

.mt-12 {
  margin-top: 12px;
}

.mb-12 {
  margin-bottom: 12px;
}
</style>
