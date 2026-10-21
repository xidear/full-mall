<script setup lang="ts">
import { formatDateTime } from '@fullmall/api-client'
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface Review {
  id: string
  skuId: string
  orderId: string
  memberId: string
  rating: number
  content: string
  status: 'PUBLISHED' | 'HIDDEN'
  createdAt: string
}

const reviews = ref<Review[]>([])
const loading = ref(false)
const error = ref('')
const statusFilter = ref<'' | 'PUBLISHED' | 'HIDDEN'>('')
const page = ref(0)
const size = ref(20)
const total = ref(0)
// 正在处理的评价 ID：避免重复点击造成状态反复
const actionId = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get<{ items: Review[]; total: number }>('/admin/v1/reviews', {
      status: statusFilter.value || undefined,
      page: page.value,
      size: size.value
    })
    reviews.value = response.items
    total.value = response.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '评价加载失败'
  } finally {
    loading.value = false
  }
}

function reloadFromFirstPage() {
  page.value = 0
  load()
}

function changePage(next: number) {
  page.value = next - 1
  load()
}

async function toggleStatus(review: Review) {
  if (actionId.value) return
  const next = review.status === 'PUBLISHED' ? 'HIDDEN' : 'PUBLISHED'
  actionId.value = review.id
  try {
    await api.post(`/admin/v1/reviews/${review.id}/status`, { status: next })
    ElMessage.success(next === 'HIDDEN' ? '已隐藏该评价' : '已恢复展示')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
  } finally {
    actionId.value = ''
  }
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>评价审核</h2>
        <p class="muted">只隐藏不删除：隐藏后前台不可见，可随时恢复。</p>
      </div>
      <div class="header-actions">
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 160px"
                   @change="reloadFromFirstPage">
          <el-option label="展示中" value="PUBLISHED" />
          <el-option label="已隐藏" value="HIDDEN" />
        </el-select>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <el-table :data="reviews" stripe>
      <el-table-column label="评分" width="140">
        <template #default="{ row }">
          <el-rate :model-value="row.rating" disabled />
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
            {{ row.status === 'PUBLISHED' ? '展示中' : '已隐藏' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button size="small" :type="row.status === 'PUBLISHED' ? 'danger' : 'success'"
                     :loading="actionId === row.id" @click="toggleStatus(row)">
            {{ row.status === 'PUBLISHED' ? '隐藏' : '恢复' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-footer">
      <span class="muted">共 {{ total }} 条评价</span>
      <el-pagination layout="prev, pager, next" :page-size="size" :total="total"
                     :current-page="page + 1" @current-change="changePage" />
    </div>

    <el-empty v-if="!loading && !reviews.length" description="暂无评价" />
  </div>
</template>
