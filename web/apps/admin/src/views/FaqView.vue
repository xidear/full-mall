<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface Candidate { question: string; hits: number; lastSeen: string }
interface Faq { id: string; question: string; answer: string }

const candidates = ref<Candidate[]>([])
const entries = ref<Faq[]>([])
const loading = ref(false)
const answer = ref('')
const activeQuestion = ref('')
const saving = ref(false)
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    candidates.value = await api.get<Candidate[]>('/admin/v1/faq/candidates?days=30&minCount=2')
    entries.value = await api.get<Faq[]>(
      `/admin/v1/faq${keyword.value ? `?keyword=${encodeURIComponent(keyword.value)}` : ''}`)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function pick(question: string) {
  activeQuestion.value = question
  answer.value = ''
}

async function adopt() {
  if (!activeQuestion.value) return
  if (!answer.value.trim()) {
    ElMessage.error('答案不能为空')
    return
  }
  saving.value = true
  try {
    await api.post('/admin/v1/faq', {
      question: activeQuestion.value,
      answer: answer.value.trim()
    })
    ElMessage.success('已加入 FAQ')
    activeQuestion.value = ''
    answer.value = ''
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
    <h1>FAQ 知识库</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="候选来自「客服没查到答案的问题」，按重复次数排序"
              description="答案必须人工写：不自动生成，避免把错答案回给用户。" />

    <div class="columns">
      <el-card shadow="never">
        <template #header>候选问题（近 30 天，≥2 次）</template>
        <el-table :data="candidates" v-loading="loading" empty-text="暂无候选"
                  highlight-current-row @current-change="(row: Candidate | null) => row && pick(row.question)">
          <el-table-column prop="question" label="问题" min-width="220" />
          <el-table-column prop="hits" label="次数" width="80" />
          <el-table-column label="最近" width="170">
            <template #default="{ row }">{{ new Date(row.lastSeen).toLocaleString() }}</template>
          </el-table-column>
        </el-table>

        <div v-if="activeQuestion" class="adopt">
          <p class="q">采纳：{{ activeQuestion }}</p>
          <el-input v-model="answer" type="textarea" :rows="4" data-testid="faq-answer"
                    placeholder="写下标准答案（客服可直接使用）" />
          <el-button type="primary" class="mt" :loading="saving" @click="adopt">加入 FAQ</el-button>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>已收录 FAQ</template>
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="搜索问题" clearable style="width: 200px" />
          <el-button :loading="loading" @click="load">搜索</el-button>
        </div>
        <el-table :data="entries" v-loading="loading" empty-text="还没有 FAQ">
          <el-table-column prop="question" label="问题" min-width="200" />
          <el-table-column prop="answer" label="答案" min-width="260" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.columns { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.adopt { margin-top: 16px; }
.q { font-weight: 600; margin: 0 0 8px; }
.mt { margin-top: 12px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 12px; }
</style>
