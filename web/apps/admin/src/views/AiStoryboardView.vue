<script setup lang="ts">
import { onMounted, ref } from 'vue'
// 默认独立页；嵌进「AI 素材生成」的标签页时传 embedded，避免出现两级标题
const props = defineProps<{ embedded?: boolean }>()
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import ProductPicker from '../components/ProductPicker.vue'
import SkuPicker from '../components/SkuPicker.vue'
import AssetPicker from '../components/AssetPicker.vue'

interface Provider { code: string; kind: string; name: string; model: string }
interface AssetRow { id: string; url: string; name: string; kind: string }
interface Shot {
  id: string
  sortOrder: number
  prompt: string
  guideAssetIds: string[]
  durationSec: number
  jobId?: string | null
  assetId?: string | null
}
interface Storyboard { id: string; title: string; productId?: string | null; shots: Shot[] }

const route = useRoute()
const providers = ref<Provider[]>([])
const assets = ref<AssetRow[]>([])
const storyboard = ref<Storyboard | null>(null)
const busy = ref(false)
const selectedProduct = ref<{ id: string; title: string; skus?: Array<{ id: string; skuCode: string }> } | null>(null)

const form = ref({
  title: '商品主图视频',
  productId: String(route.query.productId ?? ''),
  skuId: '',
  providerCode: '',
  shots: [{ prompt: '', guideAssetIds: [] as string[], durationSec: 5 }]
})

const videoProviders = () => providers.value.filter(p => p.kind === 'VIDEO')
const images = () => assets.value.filter(a => (a.kind || '').toUpperCase() !== 'VIDEO')

function onProductSelect(product: { id: string; title: string; skus?: Array<{ id: string; skuCode: string }> } | null) {
  selectedProduct.value = product
  form.value.skuId = ''
}

async function load() {
  try {
    providers.value = await api.get<Provider[]>('/admin/v1/ai/media/providers')
    // 与素材选择器统一用检索接口（过滤软删除、支持标签/关键词）；只取最近 50 条，
    // 这决定了下文 assetUrl() 的反查边界。
    const page = await api.get<{ items: AssetRow[] }>('/admin/v1/assets/search?page=0&size=50')
    assets.value = page.items ?? []
    form.value.providerCode = videoProviders()[0]?.code ?? ''
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '加载失败')
  }
}

function addShot() {
  form.value.shots.push({ prompt: '', guideAssetIds: [], durationSec: 5 })
}

function removeShot(index: number) {
  if (form.value.shots.length === 1) return
  form.value.shots.splice(index, 1)
}

async function save() {
  if (form.value.shots.some(s => !s.prompt.trim())) {
    ElMessage.error('每个镜头都要写提示词')
    return
  }
  busy.value = true
  try {
    const created = await api.post<{ storyboardId: string }>('/admin/v1/ai/storyboards', {
      title: form.value.title,
      productId: form.value.productId || null,
      skuId: form.value.skuId || null,
      shots: form.value.shots.map(s => ({
        prompt: s.prompt,
        guideAssetIds: s.guideAssetIds,
        durationSec: s.durationSec
      }))
    })
    storyboard.value = await api.get<Storyboard>(`/admin/v1/ai/storyboards/${created.storyboardId}`)
    ElMessage.success('分镜已保存')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '保存失败')
  } finally {
    busy.value = false
  }
}

async function generate() {
  if (!storyboard.value) { ElMessage.error('先保存分镜'); return }
  if (!form.value.providerCode) { ElMessage.error('没有可用的视频供应商'); return }
  busy.value = true
  try {
    await api.post(`/admin/v1/ai/storyboards/${storyboard.value.id}/generate`, {
      providerCode: form.value.providerCode
    })
    ElMessage.success('已按镜头提交生成（异步）')
    await poll()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '生成失败')
  } finally {
    busy.value = false
  }
}

// 轮询 60 次 × 4s（≈4 分钟）后放弃，交给用户手动“同步结果”；没有 WebSocket，用轮询换实现简单。
// 循环内没有 try：任一次 sync 失败都会冒泡成 generate() 里的“生成失败”，而视频可能仍在生成；
// 同理 syncNow() 完全没有 catch，后端 5xx 时页面不会有任何提示（只有控制台报错）。
async function poll() {
  if (!storyboard.value) return
  for (let i = 0; i < 60; i++) {
    await new Promise(resolve => setTimeout(resolve, 4000))
    const synced = await api.post<{ synced: number }>(
      `/admin/v1/ai/storyboards/${storyboard.value.id}/sync`, {})
    storyboard.value = await api.get<Storyboard>(`/admin/v1/ai/storyboards/${storyboard.value.id}`)
    if (storyboard.value.shots.every(shot => shot.assetId)) {
      ElMessage.success(`分镜全部完成（本次回填 ${synced.synced} 个）`)
      return
    }
    if (i === 59) ElMessage.warning('仍在生成中，稍后点「同步结果」')
  }
}

async function syncNow() {
  if (!storyboard.value) return
  const synced = await api.post<{ synced: number }>(
    `/admin/v1/ai/storyboards/${storyboard.value.id}/sync`, {})
  storyboard.value = await api.get<Storyboard>(`/admin/v1/ai/storyboards/${storyboard.value.id}`)
  ElMessage.success(`已同步 ${synced.synced} 个镜头`)
}

// 视频地址只能从“最近 50 个素材”里反查：回填的视频若不在这个窗口内（生成慢/素材多）会返回空串，
// <video> 显示空白播放器。要根治得让后端在分镜里直接返回 url。
function assetUrl(id?: string | null) {
  return assets.value.find(a => a.id === id)?.url ?? ''
}

onMounted(load)
</script>

<template>
  <div>
    <h1 v-if="!props.embedded">视频分镜</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="一个分镜本 = 多个镜头；每个镜头写提示词、选引导图（首帧）、设时长"
              description="逐镜头生成，完成后自动回填；引导图与提示词都会留痕。" />

    <el-card shadow="never" class="mb-16">
      <el-form label-width="96px">
        <el-form-item label="标题"><el-input v-model="form.title" style="width: 320px" /></el-form-item>
        <el-form-item label="商品">
          <ProductPicker v-model="form.productId" @select="onProductSelect" />
        </el-form-item>
        <el-form-item label="规格">
          <SkuPicker v-model="form.skuId" :product="selectedProduct" />
        </el-form-item>
        <el-form-item label="视频供应商">
          <el-select v-model="form.providerCode" style="width: 300px"
                     :placeholder="videoProviders().length ? '选择供应商' : '未配置视频供应商'">
            <el-option v-for="p in videoProviders()" :key="p.code" :label="`${p.name}（${p.model}）`" :value="p.code" />
          </el-select>
        </el-form-item>

        <el-form-item v-for="(shot, index) in form.shots" :key="index" :label="`镜头 ${index + 1}`">
          <div class="shot">
            <el-input v-model="shot.prompt" type="textarea" :rows="2" placeholder="画面内容、运镜（推近/旋转）、光线" />
            <div class="shot-row">
              <AssetPicker v-model="shot.guideAssetIds" multiple label="引导图/参考图（可多张）" />
              <el-input-number v-model="shot.durationSec" :min="1" :max="30" />
              <span class="muted">秒</span>
              <el-button text type="danger" @click="removeShot(index)">删除镜头</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button @click="addShot">+ 加镜头</el-button>
          <el-button type="primary" :loading="busy" @click="save">保存分镜</el-button>
          <el-button :loading="busy" @click="generate">按镜头生成</el-button>
          <el-button :disabled="!storyboard" @click="syncNow">同步结果</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="storyboard" shadow="never">
      <template #header>结果（{{ storyboard.title }}）</template>
      <div v-for="shot in storyboard.shots" :key="shot.id" class="result">
        <div>
          <strong>镜头 {{ shot.sortOrder + 1 }}</strong>
          <div class="muted">{{ shot.prompt }}</div>
          <div class="muted">引导图：{{ (shot.guideAssetIds ?? []).map(assetUrl).filter(Boolean).join(' , ') || '未选' }}</div>
        </div>
        <div>
          <video v-if="shot.assetId" :src="assetUrl(shot.assetId)" controls style="width: 260px" />
          <el-tag v-else type="warning">生成中</el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.shot { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.shot-row { display: flex; gap: 8px; align-items: center; }

.result { display: flex; justify-content: space-between; gap: 16px; padding: 12px 0; border-bottom: 1px dashed #eee; }
</style>
