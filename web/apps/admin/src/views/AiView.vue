<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { documentTypeLabels, labelOf } from '../labels'

interface AiStatus {
  code?: string
  edition?: string
  enabled?: boolean
  providerCount?: number
  ready?: boolean
  reason?: string
  missing?: string[]
  chatModel?: string
  embeddingModel?: string
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

onMounted(loadStatus)
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
          <el-descriptions-item label="召回条数">{{ status?.topK ?? '—' }}</el-descriptions-item>
        </el-descriptions>
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
      </section>
    </template>
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
