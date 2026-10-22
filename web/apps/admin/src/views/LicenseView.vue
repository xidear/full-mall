<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface LicenseStatus {
  code: string
  message: string
  customer?: string | null
  edition?: string | null
  features?: string[]
  expiresAt?: string | null
  readOnly: boolean
}

const status = ref<LicenseStatus | null>(null)
const loading = ref(false)
const uploadContent = ref('')
const uploading = ref(false)

async function load() {
  loading.value = true
  try {
    status.value = await api.get<LicenseStatus>('/admin/v1/license/status')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '许可状态读取失败')
  } finally {
    loading.value = false
  }
}

async function upload() {
  if (!uploadContent.value.trim() || uploading.value) return
  uploading.value = true
  try {
    const result = await api.post<{ status: LicenseStatus; saved: boolean }>(
      '/admin/v1/license/upload', { content: uploadContent.value })
    status.value = result.status
    if (result.status.readOnly) {
      ElMessage.warning(`已保存，但许可仍无效：${result.status.message}`)
    } else {
      ElMessage.success('许可证已更新并生效')
      uploadContent.value = ''
    }
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1>许可证</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="到期后进入只读：可查看与导出，写操作返回 402"
              description="登录、许可上传、支付回调仍放行——否则无法续费、且会丢已付款订单。" />

    <el-descriptions :column="2" border v-loading="loading" class="mb-16">
      <el-descriptions-item label="状态">{{ status?.code ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="版本">{{ status?.edition ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="客户">{{ status?.customer ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="到期时间">{{ status?.expiresAt ?? '永久 / 未设置' }}</el-descriptions-item>
      <el-descriptions-item label="模式">
        <el-tag :type="status?.readOnly ? 'danger' : 'success'">
          {{ status?.readOnly ? '只读（受限）' : '正常' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="功能">{{ status?.features?.join('、') || '—' }}</el-descriptions-item>
      <el-descriptions-item label="说明" :span="2">{{ status?.message }}</el-descriptions-item>
    </el-descriptions>

    <h3>上传新许可证</h3>
    <el-input v-model="uploadContent" type="textarea" :rows="6"
              placeholder='粘贴 license.json 内容，例如 {"payload":"...","signature":"..."}' />
    <div class="actions">
      <el-button type="primary" :loading="uploading" @click="upload">保存并立即生效</el-button>
      <el-button plain :loading="loading" @click="load">刷新状态</el-button>
    </div>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.actions { margin-top: 12px; display: flex; gap: 12px; }
</style>
