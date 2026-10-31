<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { documentTypeLabels, labelOf } from '../labels'
import { useEnums } from '../composables/useEnums'

const { options } = useEnums()

interface AiStatus {
  code?: string
  edition?: string
  enabled?: boolean
  providerCount?: number
  ready?: boolean
  reason?: string
  missing?: string[]
  chatModel?: string
  chatProvider?: string
  embeddingModel?: string
  retrieval?: string
  semanticEnabled?: boolean
  topK?: number
}

interface Citation {
  sourceId: string
  score: number
  snippet: string
}

interface AskResult {
  answer: string | null
  fallback: boolean
  reason: string | null
  citations: Citation[]
  topScore: number
}

// unavailable: 社区版没有 AI 类（404）；未启用/未配置：企业版 503
const unavailable = ref(false)
const status = ref<AiStatus | null>(null)
const loading = ref(false)

interface UnansweredRow { at: string; type: string; actor: string; question?: string | null }

/** 未命中/被拦截的问题：运营据此补知识库，而不是让同样的空白反复出现。 */
const unanswered = ref<UnansweredRow[]>([])

interface AiProviderRow {
  code: string
  kind: string
  name: string
  protocol: string
  endpoint: string
  model: string
  enabled: boolean
  priority: number
  vision: boolean
  hasKey: boolean
}

/**
 * 模型供应商管理：此前只有 PUT 接口、没有页面，运营只能改库。
 * 供应商是"对话/向量"两类文本模型；协议候选来自后端枚举（AI_PROTOCOL），不写死。
 */
const providers = ref<AiProviderRow[]>([])
const providerDialog = ref(false)
const providerSaving = ref(false)
const providerForm = reactive({
  code: '',
  name: '',
  protocol: 'openai-chat',
  endpoint: '',
  model: '',
  apiKey: '',
  enabled: true,
  priority: 100
})

/** 新建供应商的默认协议（不是配置兜底）：表单初始值，运营可在下拉里改。 */
const DEFAULT_PROVIDER_PROTOCOL = 'openai-chat'

async function loadProviders() {
  try {
    providers.value = await api.get<AiProviderRow[]>('/admin/v1/ai/providers')
  } catch (cause) {
    ElMessage.error('模型供应商加载失败：' + (cause instanceof Error ? cause.message : ''))
  }
}

function openProvider(row?: AiProviderRow) {
  providerForm.code = row?.code ?? ''
  providerForm.name = row?.name ?? ''
  providerForm.protocol = row ? row.protocol : DEFAULT_PROVIDER_PROTOCOL
  providerForm.endpoint = row?.endpoint ?? ''
  providerForm.model = row?.model ?? ''
  providerForm.apiKey = ''
  providerForm.enabled = row?.enabled ?? true
  providerForm.priority = row?.priority ?? 100
  providerDialog.value = true
}

async function saveProvider() {
  if (!providerForm.code.trim() || !providerForm.endpoint.trim() || !providerForm.model.trim()) {
    ElMessage.error('编码、endpoint、模型名都必须填写')
    return
  }
  providerSaving.value = true
  try {
    await api.put(`/admin/v1/ai/providers/${encodeURIComponent(providerForm.code.trim())}`, {
      kind: 'TEXT',
      name: providerForm.name.trim() || providerForm.code.trim(),
      protocol: providerForm.protocol,
      endpoint: providerForm.endpoint.trim(),
      model: providerForm.model.trim(),
      // 留空 = 保留原 Key（后端 COALESCE），不把已存的密钥覆盖成空
      apiKey: providerForm.apiKey.trim() || undefined,
      enabled: providerForm.enabled,
      priority: providerForm.priority,
      vision: false
    })
    ElMessage.success('供应商已保存')
    providerDialog.value = false
    await loadProviders()
    await loadStatus()
  } catch (cause) {
    ElMessage.error('保存失败：' + (cause instanceof Error ? cause.message : ''))
  } finally {
    providerSaving.value = false
  }
}

async function loadUnanswered() {
  try {
    unanswered.value = await api.get<UnansweredRow[]>('/admin/v1/ai/unanswered', { limit: 20 })
  } catch (cause) {
    ElMessage.error('未命中清单加载失败：' + (cause instanceof Error ? cause.message : ''))
  }
}

const question = ref('')
const asking = ref(false)
const result = ref<AskResult | null>(null)

const uploading = ref(false)
const imported = ref<{ chunks: number; chars: number; totalChunks: number; type: string } | null>(null)

async function loadStatus() {
  loading.value = true
  try {
    status.value = await api.get<AiStatus>('/admin/v1/ai/status')
    unavailable.value = false
  } catch (cause) {
    const message = cause instanceof Error ? cause.message : String(cause)
    // 404 = 该构建根本没注册这个模块（真·社区版/旧构建）；其余（503/网络）= 模块在但不可用。
    // 按 status 判断，不匹配文案（后端换文案会误判）。
    const status404 = (cause as { status?: number }).status === 404
    unavailable.value = status404
    status.value = status404 ? null : { reason: message }
  } finally {
    loading.value = false
  }
}

async function ask() {
  if (!question.value.trim() || asking.value) return
  asking.value = true
  result.value = null
  try {
    result.value = await api.post<AskResult>('/admin/v1/ai/ask', { question: question.value.trim() })
    await loadUnanswered()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '提问失败')
  } finally {
    asking.value = false
  }
}

async function uploadFile(file: File) {
  uploading.value = true
  imported.value = null
  try {
    const form = new FormData()
    form.append('file', file)
    const response = await api.postForm<{ chunks: number; chars: number; totalChunks: number; type: string }>(
      '/admin/v1/ai/knowledge/import', form)
    imported.value = response
    ElMessage.success(`已导入 ${response.chunks} 个知识块`)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '导入失败')
  } finally {
    uploading.value = false
  }
}

// 选完必须把 input.value 置空，否则再次选同一个文件不触发 change，表现为“点了没反应”。
function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    uploadFile(file)
    input.value = ''
  }
}

onMounted(async () => {
  await loadStatus()
  await loadProviders()
  await loadUnanswered()
})
</script>

<template>
  <div>
    <h1>AI 客服</h1>

    <!-- 两种"不可用"要分清：模块不在（社区版/旧构建）vs 模块在但没配供应商 -->
    <el-alert v-if="unavailable" type="info" show-icon :closable="false" class="mb-16"
              title="该构建未包含 AI 客服模块（community 开源构建）"
              description="内部/企业版构建包含该模块；开源版不附带模型权重，因此不注册相关路由。" />

    <el-alert v-else-if="status && status.enabled === false" type="warning" show-icon :closable="false" class="mb-16"
              :title="status.reason || '未配置 AI 供应商'"
              :description="'当前形态：' + (status.edition ?? 'unknown') + '；配置好后回到本页即可使用。'">
      <el-button link type="primary" @click="$router.push('/integrations')">去「第三方服务」配置供应商</el-button>
    </el-alert>

    <el-alert v-else-if="status && !status.ready" type="warning" show-icon :closable="false" class="mb-16"
              :title="status.reason || 'AI 未就绪'"
              :description="status.missing?.length ? '缺少配置：' + status.missing.join('、') : ''" />

    <template v-if="!unavailable">
      <section class="panel">
        <h2>状态</h2>
        <el-descriptions :column="3" border v-loading="loading">
          <el-descriptions-item label="形态">{{ status?.edition ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="已启用">{{ status?.enabled ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="就绪">{{ status?.ready ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="对话模型">{{ status?.chatModel ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="嵌入模型">{{ status?.embeddingModel ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="检索方式">{{ status?.retrieval === 'SEMANTIC' ? '语义（向量）' : '关键词（无需向量服务）' }}</el-descriptions-item>
          <el-descriptions-item label="召回条数">{{ status?.topK ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="缺少配置">{{ status?.missing?.length ? status.missing.join('、') : '无' }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h2>模型供应商</h2>
            <p class="muted">
              对话模型用 <code>openai-chat</code>，向量模型用 <code>openai-embedding</code>。
              API Key 加密保存、不回显；编辑时留空表示不修改。
            </p>
          </div>
          <el-button @click="openProvider()">添加供应商</el-button>
        </div>
        <el-table :data="providers" size="small" empty-text="尚未配置供应商">
          <el-table-column prop="code" label="标识" width="170" />
          <el-table-column prop="name" label="名称" width="150" />
          <el-table-column prop="protocol" label="协议" width="170" />
          <el-table-column prop="model" label="模型" width="180" />
          <el-table-column prop="endpoint" label="地址" min-width="220" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="密钥" width="80">
            <template #default="{ row }">{{ row.hasKey ? '已配置' : '无' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" @click="openProvider(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="panel">
        <h2>导入客户资料</h2>
        <p class="muted">
          支持 pdf / docx / txt / md / csv / html / xlsx。图片与扫描件会被明确拒绝；
          同一文档重复导入会替换旧知识，不会重复堆叠。
        </p>
        <label class="upload-btn">
          <input type="file" accept=".pdf,.docx,.doc,.txt,.md,.csv,.html,.htm,.xlsx,.xls"
                 :disabled="uploading" @change="onFileChange" />
          <span>{{ uploading ? '导入中…' : '选择文件并导入' }}</span>
        </label>
        <el-descriptions v-if="imported" :column="3" border class="mt-16">
          <el-descriptions-item label="类型">{{ labelOf(documentTypeLabels, imported.type) }}</el-descriptions-item>
          <el-descriptions-item label="知识块">{{ imported.chunks }}</el-descriptions-item>
          <el-descriptions-item label="总块数">{{ imported.totalChunks }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="panel">
        <h2>提问测试</h2>
        <div class="ask-row">
          <el-input v-model="question" placeholder="例如：鲜奶可以退货吗？"
                    :disabled="asking" @keyup.enter="ask" />
          <el-button type="primary" :loading="asking" @click="ask">提问</el-button>
        </div>

        <template v-if="result">
          <el-alert class="mt-16" :type="result.fallback ? 'warning' : 'success'" :closable="false" show-icon
                    :title="result.fallback ? '未能给出可靠回答（已建议转人工）' : '回答'" />
          <p v-if="result.answer" class="answer">{{ result.answer }}</p>
          <p v-else class="violations">
            知识库没有足够依据，已建议转人工{{ result.reason ? '（原因：' + result.reason + '，最高相关度 ' + result.topScore.toFixed(2) + '）' : '' }}
          </p>

          <h3>引用来源</h3>
          <el-table :data="result.citations" size="small" empty-text="无引用（说明没有依据）">
            <el-table-column prop="sourceId" label="知识条目" width="200" />
            <el-table-column prop="score" label="相关度" width="100">
              <template #default="{ row }">{{ row.score.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="snippet" label="命中片段" min-width="320" show-overflow-tooltip />
          </el-table>
        </template>
        <h3>未命中问题（补知识库用）</h3>
        <el-table :data="unanswered" size="small" empty-text="暂无未命中记录">
          <el-table-column prop="at" label="时间" width="180" />
          <el-table-column prop="type" label="类型" width="200">
            <template #default="{ row }">
              {{ row.type === 'AI_QUESTION_BLOCKED' ? '被拦截（不当内容）' : '知识库无依据' }}
            </template>
          </el-table-column>
          <el-table-column prop="actor" label="提问人" width="140" />
          <el-table-column prop="question" label="问题" min-width="320" show-overflow-tooltip />
        </el-table>
      </section>
    </template>

    <el-dialog v-model="providerDialog" :title="providerForm.code ? '编辑模型供应商' : '添加模型供应商'" width="560px">
      <el-form label-width="100px">
        <el-form-item label="标识">
          <el-input v-model="providerForm.code" placeholder="例如 company-chat / local-embedding" maxlength="64" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="providerForm.name" placeholder="例如 公司自营对话模型" maxlength="64" />
        </el-form-item>
        <el-form-item label="协议">
          <el-select v-model="providerForm.protocol" style="width: 100%">
            <el-option v-for="option in options('AI_PROTOCOL')" :key="option.code"
                       :label="option.label" :value="option.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="服务地址">
          <el-input v-model="providerForm.endpoint" placeholder="例如 https://company.binnarui.com:8500/v1" />
        </el-form-item>
        <el-form-item label="模型名">
          <el-input v-model="providerForm.model" placeholder="例如 deepseek-v4.1-flash" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="providerForm.apiKey" type="password" show-password
                    :placeholder="providerForm.code ? '留空表示不修改' : '服务端不需要鉴权时留空'" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="providerForm.enabled" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="providerForm.priority" :min="0" :max="9999" />
          <span class="muted" style="margin-left: 8px">数字越小越优先</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="providerDialog = false">取消</el-button>
        <el-button type="primary" :loading="providerSaving" @click="saveProvider">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.panel { margin-bottom: 16px; }
.ask-row { display: flex; gap: 12px; }
.answer { margin-top: 12px; line-height: 1.7; white-space: pre-wrap; }
.violations { color: var(--el-color-danger); margin-top: 8px; }
.upload-btn { display: inline-block; padding: 8px 16px; border: 1px dashed var(--el-border-color);
  border-radius: 6px; cursor: pointer; }
.upload-btn input { display: none; }
.mb-16 { margin-bottom: 16px; }
.mt-16 { margin-top: 16px; }
</style>
