<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

interface Uploaded { url: string; key: string; bytes: number; provider: string }

const uploaded = ref<Uploaded[]>([])
const uploading = ref(false)

async function handleFile(file: File) {
  uploading.value = true
  try {
    const form = new FormData()
    form.append('file', file)
    const response = await fetch(
      `${import.meta.env.VITE_API_BASE || 'http://localhost:8080'}/admin/v1/media`, {
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
</script>

<template>
  <div>
    <h1>素材库</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="只允许图片（png/jpeg/webp/gif），单文件上限由后端配置（默认 5MB）"
              description="存储走七牛/COS/本地，具体取决于后台「第三方服务」里的存储配置。" />

    <div class="mb-16">
      <label class="upload-button">
        <input type="file" accept="image/png,image/jpeg,image/webp,image/gif"
               :disabled="uploading" @change="onChange" />
        <span>{{ uploading ? '上传中…' : '选择图片上传' }}</span>
      </label>
    </div>

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
.upload-button {
  display: inline-block; padding: 8px 16px; border-radius: 6px;
  background: #409eff; color: #fff; cursor: pointer;
}
.upload-button input { display: none; }
.thumb { width: 72px; height: 72px; object-fit: cover; border-radius: 4px; }
</style>
