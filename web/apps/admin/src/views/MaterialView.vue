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
const purposeFilter = ref('')
const loading = ref(false)
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
      <el-button type="primary" plain @click="$router.push('/ai-media')">AI 生成素材</el-button>
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
          <el-button link type="primary" @click="variantFrom(row)">用它生成变体</el-button>
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
</style>
