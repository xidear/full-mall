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
  multiProvider?: boolean
  providers: Provider[]
}

interface Integration {
  capability: string
  provider: string
  enabled: boolean
  hint: Record<string, unknown>
  secretsConfigured: boolean
}

/**
 * 通道路由：哪个业务动作用哪家供应商。
 * 候选（可用供应商）由后端按「已配置且已启用」算出来，页面不猜。
 */
interface UsageRoute {
  capability: string
  key: string
  label: string
  description: string
  provider?: string
  recipient?: string
  needsRecipient: boolean
  availableProviders: string[]
}

const capabilities = ref<Capability[]>([])
const rows = ref<Integration[]>([])
const usages = ref<UsageRoute[]>([])
const routesLoading = ref(false)
const busyUsage = ref('')
// 收件人只在「已绑定但还没填过」时可编辑；绑定时提交，避免每次输入都发请求
const recipients = ref<Record<string, string>>({})
// 未绑定行里先选好的供应商：开关一打开就用它绑定（否则页面显示 A、实际绑 B）
const pendingProviders = ref<Record<string, string>>({})
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

async function loadRoutes() {
  routesLoading.value = true
  try {
    usages.value = await api.get<UsageRoute[]>('/admin/v1/channel-routes')
    for (const usage of usages.value) {
      if (usage.recipient) recipients.value[usageId(usage)] = usage.recipient
    }
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '通道路由加载失败')
  } finally {
  routesLoading.value = false
  }
}

function usageId(usage: UsageRoute): string {
  return usage.capability + '/' + usage.key
}

function providerLabel(usage: UsageRoute, code?: string): string {
  if (!code) return ''
  const capability = capabilities.value.find(item => item.code === usage.capability)
  return capability?.providers.find(item => item.code === code)?.label ?? code
}

/**
 * 绑定 / 改绑 / 解绑都在列表里直接完成（AGENTS §13.1）。
 * 失败时保持原状态并报原因：不允许「先翻转再回滚」那种视觉欺骗。
 */
async function toggleRoute(usage: UsageRoute, bound: boolean) {
  const id = usageId(usage)
  if (busyUsage.value) return
  busyUsage.value = id
  try {
    if (!bound) {
      await api.delete('/admin/v1/channel-routes/' + usage.capability + '/' + usage.key)
      ElMessage.success('已取消绑定：' + usage.label)
    } else {
      const provider = pendingProviders.value[id] ?? usage.provider ?? usage.availableProviders[0]
      if (!provider) {
        ElMessage.error('该能力尚未配置并启用任何供应商，请先在上方保存供应商凭据')
        return
      }
      const recipient = (recipients.value[id] ?? '').trim()
      await api.put('/admin/v1/channel-routes/' + usage.capability + '/' + usage.key, {
        provider,
        recipient: recipient || undefined
      })
      ElMessage.success('已绑定：' + usage.label + ' → ' + providerLabel(usage, provider))
    }
    await loadRoutes()
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, '保存失败'))
  } finally {
    busyUsage.value = ''
  }
}

async function changeProvider(usage: UsageRoute, provider: string) {
  const id = usageId(usage)
  if (!usage.provider) {
    // 未绑定的行只记住选择，真正生效要点开关，避免误绑
    pendingProviders.value[id] = provider
    return
  }
  busyUsage.value = id
  try {
    await api.put('/admin/v1/channel-routes/' + usage.capability + '/' + usage.key, {
      provider,
      recipient: (recipients.value[id] ?? '').trim() || undefined
    })
    ElMessage.success('已改绑：' + usage.label + ' → ' + providerLabel(usage, provider))
    await loadRoutes()
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, '改绑失败'))
    await loadRoutes()
  } finally {
    busyUsage.value = ''
  }
}

async function testRoute(usage: UsageRoute) {
  busyUsage.value = usageId(usage)
  try {
    const result = await api.post<{ provider: string, mock: boolean }>('/admin/v1/channel-routes/test', {
      capability: usage.capability,
      usageKey: usage.key,
      recipient: (recipients.value[usageId(usage)] ?? '').trim() || undefined
    })
    ElMessage.success(result.mock
      ? '测试完成：当前为演示模式（' + result.provider + '），消息未真正发送'
      : '测试消息已通过 ' + result.provider + ' 发出')
    await loadRoutes()
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause, '测试发送失败'))
  } finally {
    busyUsage.value = ''
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
  // 没有可填字段的供应商（例如演示占位 mock）本来就没有配置项：
  // 这里强行要求「至少一项」会让它永远保存不了。
  if (fields.value.length > 0 && Object.keys(config).length === 0) {
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
  await loadRoutes()
})
</script>

<template>
  <div>
    <h1>第三方服务</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="密钥只加密入库，页面永不回显"
              description="能力、服务商与配置项都从后端清单选择，不需要手填键名；键名含 secret/key/token/password 的字段按敏感处理，留空表示沿用旧值。短信/邮件/公众号/告警群可以同时启用多家供应商，具体哪个动作用哪家，由下方「通道路由」决定。" />

    <el-alert v-if="catalogError" :title="catalogError" type="error" show-icon :closable="false" class="mb-16" />

    <el-form v-if="capabilities.length > 0" label-width="150px" style="max-width: 780px" class="integration-form">
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
        <span class="field-hint">
          {{ capability?.multiProvider
            ? '该能力支持多家并存；停用其中一家只影响绑定到它的用途'
            : '单供应商能力：启用后同能力的其它服务商会自动停用' }}
        </span>
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

    <h2 class="section-title">通道路由</h2>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="哪个业务动作用哪家供应商"
              description="供应商配好并启用后，在这里按用途勾选。没有绑定的用途在正式环境会明确失败（不会偷偷用其它供应商），加急告警类用途必须填收件人。" />

    <el-table :data="usages" v-loading="routesLoading" empty-text="尚未登记任何通信用途">
      <el-table-column label="用途" min-width="200">
        <template #default="{ row }">
          {{ row.label }}
          <div class="muted-code">{{ row.description }}</div>
        </template>
      </el-table-column>
      <el-table-column label="供应商" width="220">
        <template #default="{ row }">
          <el-select v-if="row.availableProviders.length > 0"
                     :model-value="row.provider ?? pendingProviders[row.capability + '/' + row.key] ?? ''"
                     :placeholder="row.provider ? '选择供应商' : '未绑定'"
                     :disabled="busyUsage === row.capability + '/' + row.key"
                     style="width: 100%" @change="(value: string) => changeProvider(row, value)">
            <el-option v-for="code in row.availableProviders" :key="code"
                       :label="providerLabel(row, code) + '（' + code + '）'" :value="code" />
          </el-select>
          <span v-else class="muted-code">未配置供应商</span>
        </template>
      </el-table-column>
      <el-table-column label="收件人" width="240">
        <template #default="{ row }">
          <el-input v-if="row.needsRecipient" v-model="recipients[row.capability + '/' + row.key]"
                    :placeholder="row.needsRecipient ? '手机号/邮箱，告警送达地址' : ''"
                    :disabled="busyUsage === row.capability + '/' + row.key" />
          <span v-else class="muted-code">不需要</span>
        </template>
      </el-table-column>
      <el-table-column label="已绑定" width="110">
        <template #default="{ row }">
          <el-switch :model-value="!!row.provider"
                     :loading="busyUsage === row.capability + '/' + row.key"
                     :disabled="!row.provider && row.availableProviders.length === 0"
                     @change="(value: boolean) => toggleRoute(row, value)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button size="small" :disabled="!row.provider"
                     :loading="busyUsage === row.capability + '/' + row.key"
                     @click="testRoute(row)">测试发送</el-button>
        </template>
      </el-table-column>
    </el-table>
    <p class="muted route-scroll-hint">窄屏下表格可左右滑动查看收件人与操作列。</p>
  </div>
</template>

<style scoped>
/* 手机宽度下 150px 的标签列会把输入框挤到 160px 左右：占位提示被截断、字段说明一字一行。
   小屏改成标签独占一行，输入框吃满宽度（只作用于本页的供应商配置表单，不动全站 el-form 默认值）。 */

/* 只在表格真的溢出时提示：宽屏一行放得下就不必教人滑动。 */
.route-scroll-hint {
  display: none;
}

@media (max-width: 640px) {
  .route-scroll-hint {
    display: block;
    margin: 6px 0 0;
    font-size: 12px;
  }
}

@media (max-width: 640px) {
  .integration-form :deep(.el-form-item) {
    display: block;
  }

  .integration-form :deep(.el-form-item__label) {
    display: block;
    width: auto;
    padding: 0 0 4px;
    text-align: left;
  }

  .integration-form :deep(.el-form-item__content) {
    margin-left: 0;
  }
}

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

.section-title {
  margin: 28px 0 12px;
  font-size: 16px;
}
</style>
