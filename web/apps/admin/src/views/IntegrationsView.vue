<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { apiErrorMessage } from '../composables/useAsyncState'

/**
 * 第三方服务配置。
 *
 * <p>能力、服务商、配置项**全部来自后端清单**（GET /admin/v1/integrations/schema）：
 * 这里没有一处硬编码候选，也没有任何让运营手填键名的输入框。
 * 手填服务商/键名的代价是"填错一个字就得到一条永不生效的死配置"，而且运营根本不知道该填什么。
 */
interface Field {
  key: string
  label: string
  secret: boolean
  required: boolean
  hint: string
}

interface Provider {
  code: string
  label: string
  fields: Field[]
}

interface Capability {
  code: string
  label: string
  description: string
  providers: Provider[]
}

interface Integration {
  capability: string
  provider: string
  enabled: boolean
  hint: Record<string, unknown>
  secretsConfigured: boolean
}

const capabilities = ref<Capability[]>([])
const rows = ref<Integration[]>([])
const loading = ref(false)
const saving = ref(false)
const catalogError = ref('')
const form = ref({ capability: '', provider: '', enabled: false, values: {} as Record<string, string> })

const capability = computed(() => capabilities.value.find(item => item.code === form.value.capability))
const providers = computed(() => capability.value?.providers ?? [])
const selectedProvider = computed(() => providers.value.find(item => item.code === form.value.provider))
const fields = computed(() => selectedProvider.value?.fields ?? [])

// 换能力时服务商必须跟着换（不同能力没有可比的服务商），默认选中第一家
watch(() => form.value.capability, () => {
  form.value.provider = providers.value[0]?.code ?? ''
})

// 换服务商清空已填字段：两个服务商的键不同，留着旧值只会让人误以为要一起提交
watch(() => form.value.provider, () => {
  form.value.values = {}
})

async function loadCatalog() {
  catalogError.value = ''
  try {
    capabilities.value = await api.get<Capability[]>('/admin/v1/integrations/schema')
    if (!form.value.capability && capabilities.value.length > 0) {
      form.value.capability = capabilities.value[0].code
    }
  } catch (cause) {
    catalogError.value = '候选清单加载失败，无法保存：' + apiErrorMessage(cause, '未知原因')
  }
}

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

async function save() {
  if (saving.value) return
  if (!form.value.capability || !form.value.provider) {
    ElMessage.error('请先选择能力与服务商')
    return
  }
  const config: Record<string, string> = {}
  for (const field of fields.value) {
    const value = (form.value.values[field.key] ?? '').trim()
    // 敏感字段留空 = 沿用旧值（服务端语义），因此这里只是"不提交该键"
    if (value) config[field.key] = value
  }
  if (Object.keys(config).length === 0) {
    ElMessage.error('至少填写一项配置（密钥可留空沿用旧值，但首次配置必须填）')
    return
  }
  saving.value = true
  try {
    await api.post('/admin/v1/integrations', {
      capability: form.value.capability,
      provider: form.value.provider,
      config,
      enabled: form.value.enabled
    })
    ElMessage.success('已保存（敏感字段已加密，不会回显）')
    form.value.values = {}
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadCatalog()
  await load()
})
</script>

<template>
  <div>
    <h1>第三方服务</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="密钥只加密入库，页面永不回显"
              description="能力、服务商与配置项都从后端清单选择，不需要手填键名；键名含 secret/key/token/password 的字段按敏感处理，留空表示沿用旧值。同一能力只能启用一个服务商。" />

    <el-alert v-if="catalogError" :title="catalogError" type="error" show-icon :closable="false" class="mb-16" />

    <el-form v-if="capabilities.length > 0" label-width="150px" style="max-width: 780px">
      <el-form-item label="能力">
        <el-select v-model="form.capability" style="width: 260px">
          <el-option v-for="item in capabilities" :key="item.code"
                     :label="item.label + '（' + item.code + '）'" :value="item.code" />
        </el-select>
        <span v-if="capability" class="field-hint">{{ capability.description }}</span>
      </el-form-item>

      <el-form-item label="服务商">
        <el-select v-model="form.provider" style="width: 260px" placeholder="选择服务商">
          <el-option v-for="item in providers" :key="item.code"
                     :label="item.label + '（' + item.code + '）'" :value="item.code" />
        </el-select>
      </el-form-item>

      <el-form-item v-for="field in fields" :key="field.key" :label="field.label" :required="field.required">
        <div class="field-block">
          <el-input v-model="form.values[field.key]" :type="field.secret ? 'password' : 'text'"
                    :show-password="field.secret" autocomplete="off"
                    :placeholder="field.secret ? '加密保存；留空表示沿用旧值' : '填写：' + field.hint" />
          <span class="field-hint">
            <code>{{ field.key }}</code>
            {{ field.hint }}
          </span>
        </div>
      </el-form-item>

      <el-form-item label="启用">
        <el-switch v-model="form.enabled" />
        <span class="field-hint">启用后同能力的其它服务商会自动停用</span>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" :disabled="!selectedProvider" @click="save">保存</el-button>
        <el-button :loading="loading" @click="load">刷新列表</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" empty-text="尚未配置任何第三方服务">
      <el-table-column label="能力" width="150">
        <template #default="{ row }">
          {{ capabilities.find(item => item.code === row.capability)?.label ?? row.capability }}
          <code class="muted-code">{{ row.capability }}</code>
        </template>
      </el-table-column>
      <el-table-column label="服务商" width="150">
        <template #default="{ row }">
          {{ capabilities.flatMap(item => item.providers).find(item => item.code === row.provider)?.label ?? row.provider }}
          <code class="muted-code">{{ row.provider }}</code>
        </template>
      </el-table-column>
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
.mb-16 {
  margin-bottom: 16px;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 420px;
}

.field-hint {
  margin-left: 8px;
  color: #8a8f8a;
  font-size: 12px;
}

.field-block .field-hint {
  margin-left: 0;
}

.muted-code {
  margin-left: 6px;
  color: #a8aeb8;
  font-size: 11px;
}
</style>
