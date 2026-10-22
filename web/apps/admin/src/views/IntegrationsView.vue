<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface Integration {
  capability: string
  provider: string
  enabled: boolean
  hint: Record<string, unknown>
  secretsConfigured: boolean
}

const capabilities = ['sms', 'payment', 'storage', 'moderation', 'logistics', 'ai']
const rows = ref<Integration[]>([])
const loading = ref(false)
const saving = ref(false)
const form = ref({ capability: 'sms', provider: '', enabled: false, pairs: '' })

async function load() {
  loading.value = true
  try {
    rows.value = await api.get<Integration[]>('/admin/v1/integrations')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function parsePairs(text: string): Record<string, string> {
  const result: Record<string, string> = {}
  for (const line of text.split('\n')) {
    const trimmed = line.trim()
    if (!trimmed) continue
    const index = trimmed.indexOf('=')
    if (index <= 0) {
      throw new Error(`格式应为 key=value：${trimmed}`)
    }
    result[trimmed.slice(0, index).trim()] = trimmed.slice(index + 1).trim()
  }
  return result
}

async function save() {
  if (saving.value) return
  if (!form.value.provider.trim()) {
    ElMessage.error('请填写服务商标识（如 aliyun / tencent / qiniu）')
    return
  }
  saving.value = true
  try {
    await api.post('/admin/v1/integrations', {
      capability: form.value.capability,
      provider: form.value.provider.trim(),
      config: parsePairs(form.value.pairs),
      enabled: form.value.enabled
    })
    ElMessage.success('已保存（敏感字段已加密，不会回显）')
    form.value.pairs = ''
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1>第三方服务</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="密钥只加密入库，页面永不回显"
              description="键名含 secret/key/token/password 的字段按敏感处理；留空表示沿用旧值。同一能力只能启用一个服务商。" />

    <el-form label-width="110px" style="max-width: 720px">
      <el-form-item label="能力">
        <el-select v-model="form.capability" style="width: 200px">
          <el-option v-for="item in capabilities" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="服务商">
        <el-input v-model="form.provider" placeholder="aliyun / tencent / qiniu / local" style="width: 260px" />
      </el-form-item>
      <el-form-item label="配置项">
        <el-input v-model="form.pairs" type="textarea" :rows="6"
                  placeholder="每行一个 key=value，例如：&#10;access-key=xxx&#10;secret-key=xxx&#10;sign-name=示例" />
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model="form.enabled" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button :loading="loading" @click="load">刷新列表</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" empty-text="尚未配置任何第三方服务">
      <el-table-column prop="capability" label="能力" width="140" />
      <el-table-column prop="provider" label="服务商" width="140" />
      <el-table-column label="启用" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="密钥" width="110">
        <template #default="{ row }">{{ row.secretsConfigured ? '已配置' : '无' }}</template>
      </el-table-column>
      <el-table-column label="非敏感配置" min-width="240">
        <template #default="{ row }">
          <code v-for="(value, key) in row.hint" :key="key">{{ key }}={{ value }} </code>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
code { margin-right: 10px; color: #606266; }
</style>
