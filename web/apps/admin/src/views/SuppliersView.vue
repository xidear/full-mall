<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { SupplierCredentialView } from '@fullmall/api-client'
import { api } from '../api'

const suppliers = ref<SupplierCredentialView[]>([])
const loading = ref(false)
const error = ref('')
const credentialDialog = ref(false)
const created = ref<{ supplierId: string; accessKey: string; secret: string } | null>(null)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    suppliers.value = await api.get<SupplierCredentialView[]>('/admin/v1/suppliers')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '供应商加载失败'
  } finally {
    loading.value = false
  }
}

async function rotate(supplier: SupplierCredentialView) {
  loading.value = true
  error.value = ''
  try {
    created.value = await api.post<{ supplierId: string; accessKey: string; secret: string }>(
      `/admin/v1/suppliers/${supplier.supplierId}/credentials`
    )
    credentialDialog.value = true
    await load()
    ElMessage.success('凭证已生成，旧凭证立即失效')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '凭证生成失败'
  } finally {
    loading.value = false
  }
}

async function copy(text: string, label: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`${label}已复制`)
  } catch {
    ElMessage.warning('浏览器拒绝剪贴板访问，请手动选择复制')
  }
}
</script>

<template>
  <div v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <div class="panel">
      <div class="panel-header">
        <div>
          <h2>供应商与开放接口凭证</h2>
          <p class="muted">
            供应商使用 Access Key + Secret 调用 `/open/v1`；每个请求使用 HMAC-SHA256 签名。
            Secret 只在生成时显示一次。
          </p>
        </div>
        <el-button @click="load">刷新</el-button>
      </div>

      <el-table :data="suppliers" empty-text="暂无供应商">
        <el-table-column prop="code" label="供应商编码" width="150" />
        <el-table-column prop="name" label="供应商名称" min-width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Access Key" min-width="240">
          <template #default="{ row }">
            <code>{{ row.accessKey || '尚未生成' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="最近轮换" width="190">
          <template #default="{ row }">
            {{ row.rotatedAt ? new Date(row.rotatedAt).toLocaleString('zh-CN') : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="rotate(row)">
              {{ row.hasCredential ? '轮换凭证' : '生成凭证' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="credentialDialog" title="供应商凭证（仅显示一次）" width="620px">
      <el-alert
        title="请立即保存 Secret。关闭后服务端只能验证，无法再次显示明文。"
        type="warning"
        show-icon
        :closable="false"
        class="mb-16"
      />
      <div v-if="created" class="credential-block">
        <div class="credential-row">
          <span>Access Key</span>
          <code>{{ created.accessKey }}</code>
          <el-button size="small" @click="copy(created.accessKey, 'Access Key')">复制</el-button>
        </div>
        <div class="credential-row">
          <span>Secret</span>
          <code>{{ created.secret }}</code>
          <el-button size="small" @click="copy(created.secret, 'Secret')">复制</el-button>
        </div>
      </div>
      <el-alert
        title="供应商门户 http://localhost:5175 输入这两个值即可连接；正式环境请通过 HTTPS 使用。"
        type="info"
        :closable="false"
      />
      <template #footer>
        <el-button type="primary" @click="credentialDialog = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.credential-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.credential-row {
  display: grid;
  grid-template-columns: 100px 1fr auto;
  align-items: center;
  gap: 10px;
}

.credential-row code {
  overflow-wrap: anywhere;
  padding: 8px 10px;
  border-radius: 6px;
  background: #f6f4ee;
  font-size: 12px;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>
