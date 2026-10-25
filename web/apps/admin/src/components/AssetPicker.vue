<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface Asset { id: string; name: string; url: string; kind: string; purpose?: string }

const props = withDefaults(defineProps<{
  modelValue: string | string[]
  multiple?: boolean
  accept?: 'IMAGE' | 'VIDEO' | 'ANY'
  label?: string
  valueType?: 'id' | 'url'
}>(), { multiple: false, accept: 'IMAGE', label: '选择素材', valueType: 'id' })

const emit = defineEmits<{ 'update:modelValue': [value: string | string[]] }>()

const dialog = ref(false)
const loading = ref(false)
const uploading = ref(false)
const assets = ref<Asset[]>([])
const keyword = ref('')
const selected = ref<string[]>([])
const page = ref(0)
const hasMore = ref(true)
const loadingMore = ref(false)

const chosen = computed<string[]>(() => {
  const value = props.modelValue
  return Array.isArray(value) ? value : (value ? [value] : [])
})

const filtered = computed(() => assets.value.filter(asset => {
  const purpose = asset.purpose ?? 'PRODUCT'
  if (purpose === 'OTHER_SYSTEM') return false
  if (props.accept === 'IMAGE' && (asset.kind || '').toUpperCase() === 'VIDEO') return false
  if (props.accept === 'VIDEO' && (asset.kind || '').toUpperCase() !== 'VIDEO') return false
  return !keyword.value || asset.name.toLowerCase().includes(keyword.value.toLowerCase())
}))

const selectedAssets = computed(() => assets.value.filter(asset =>
  props.valueType === 'url' ? chosen.value.includes(asset.url) : chosen.value.includes(asset.id)))

async function load() {
  loading.value = true
  page.value = 0
  hasMore.value = true
  try {
    const result = await api.get<{ content: Asset[]; last?: boolean; totalPages?: number }>(
      '/admin/v1/assets?size=60&page=0&sort=createdAt,desc')
    assets.value = result.content ?? []
    hasMore.value = result.last === undefined
      ? (result.content ?? []).length >= 60
      : !result.last
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '素材加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  loadingMore.value = true
  try {
    const next = page.value + 1
    const result = await api.get<{ content: Asset[]; last?: boolean }>(
      `/admin/v1/assets?size=60&page=${next}&sort=createdAt,desc`)
    assets.value = [...assets.value, ...(result.content ?? [])]
    page.value = next
    hasMore.value = result.last === undefined
      ? (result.content ?? []).length >= 60
      : !result.last
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '加载更多失败')
  } finally {
    loadingMore.value = false
  }
}

function open() {
  selected.value = assets.value
    .filter(asset => chosen.value.includes(props.valueType === 'url' ? asset.url : asset.id))
    .map(asset => asset.id)
  dialog.value = true
}

function toggle(id: string) {
  if (props.multiple) {
    const index = selected.value.indexOf(id)
    if (index >= 0) selected.value.splice(index, 1)
    else selected.value.push(id)
  } else {
    selected.value = [id]
  }
}

function toOutput(ids: string[]): string | string[] {
  if (props.valueType === 'url') {
    const urls = ids.map(id => assets.value.find(asset => asset.id === id)?.url ?? '').filter(Boolean)
    return props.multiple ? urls : (urls[0] ?? '')
  }
  return props.multiple ? ids : (ids[0] ?? '')
}

function confirm() {
  emit('update:modelValue', toOutput(selected.value))
  dialog.value = false
}

async function upload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const form = new FormData()
    form.append('file', file)
    const response = await fetch(
      `${import.meta.env.VITE_API_BASE || 'http://localhost:8080'}/admin/v1/media?purpose=PRODUCT`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${localStorage.getItem('fullmall.admin.token') ?? ''}` },
        body: form
      })
    if (!response.ok) throw new Error(await response.text())
    const result = await response.json() as { assetId?: string }
    if (!result.assetId) throw new Error('上传未返回素材 id')
    await load()
    selected.value = props.multiple ? [...selected.value, result.assetId] : [result.assetId]
    ElMessage.success('已上传并选中')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function clear() {
  emit('update:modelValue', props.multiple ? [] : '')
}

onMounted(load)
</script>

<template>
  <div class="asset-picker">
    <div v-if="chosen.length" class="preview-row">
      <img v-for="asset in selectedAssets" :key="asset.id" :src="asset.url"
           :alt="asset.name" class="preview" loading="lazy" />
      <span v-if="!selectedAssets.length" class="hint">已选 {{ chosen.length }} 项</span>
    </div>
    <div class="actions">
      <el-button size="small" :aria-label="label" @click="open">
        {{ chosen.length && !multiple ? '更换素材' : label }}
      </el-button>
      <el-button v-if="chosen.length" size="small" text @click="clear">清除</el-button>
    </div>

    <el-dialog v-model="dialog" title="选择素材" width="720px">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="按名称搜索" clearable style="width: 220px" />
        <label class="upload">
          <input type="file" accept="image/*" :disabled="uploading" @change="upload" />
          <span>{{ uploading ? '上传中…' : '上传新素材' }}</span>
        </label>
        <span class="hint">只展示商品/规格素材（审批附件等不在此处）</span>
      </div>

      <div v-if="loading" class="hint">加载中…</div>
      <div v-else-if="!filtered.length" class="empty">素材库没有可用素材，先上传一张</div>
      <div v-else class="grid">
        <button v-for="asset in filtered" :key="asset.id" type="button" class="cell"
                :class="{ picked: selected.includes(asset.id) }"
                :aria-pressed="selected.includes(asset.id)"
                :alt="asset.name" @click="toggle(asset.id)">
          <img :src="asset.url" :alt="asset.name" loading="lazy" />
          <span class="name">{{ asset.name }}</span>
        </button>
      </div>
      <div v-if="hasMore && !loading" class="more">
        <el-button size="small" :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>

      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="confirm">确定（已选 {{ selected.length }}）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.preview-row { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.preview { width: 64px; height: 64px; object-fit: cover; border-radius: 6px; border: 1px solid #e2e8f0; }
.actions { display: flex; gap: 8px; margin-top: 6px; }
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.upload { display: inline-flex; align-items: center; padding: 6px 12px; border-radius: 6px;
  border: 1px dashed #94a3b8; cursor: pointer; font-size: 13px; color: #475269; }
.upload input { display: none; }
.hint { color: #94a3b8; font-size: 12px; }
.empty { padding: 40px 0; text-align: center; color: #94a3b8; }
.more { text-align: center; margin-top: 12px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px;
  max-height: 52vh; overflow-y: auto; }
.cell { padding: 0; border: 2px solid transparent; border-radius: 8px; background: #f8fafc;
  cursor: pointer; display: flex; flex-direction: column; overflow: hidden; min-height: 44px; }
.cell.picked { border-color: #2563eb; }
.cell img { width: 100%; height: 92px; object-fit: cover; }
.cell .name { font-size: 12px; padding: 4px 6px; color: #475569; text-align: left;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
