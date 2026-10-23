<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface Product { id: string; title: string }
interface Translation {
  productId: string
  locale: string
  title: string
  description: string | null
  status: string
}

const products = ref<Product[]>([])
const loading = ref(false)
const keyword = ref('')
const dialog = ref(false)
const current = ref<Product | null>(null)
const locale = ref('en')
const title = ref('')
const description = ref('')
const translation = ref<Translation | null>(null)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await api.get<{ items: Product[] }>(
      `/admin/v1/products?page=0&size=50${keyword.value ? `&keyword=${encodeURIComponent(keyword.value)}` : ''}`)
    products.value = page.items ?? []
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '加载商品失败')
  } finally {
    loading.value = false
  }
}

async function open(product: Product) {
  current.value = product
  title.value = ''
  description.value = ''
  translation.value = null
  dialog.value = true
  await refreshTranslation(product.id)
}

async function refreshTranslation(productId: string) {
  try {
    translation.value = await api.get<Translation>(
      `/admin/v1/products/${productId}/translations/${locale.value}`)
    title.value = translation.value.title
    description.value = translation.value.description ?? ''
  } catch {
    // 还没有译文：保持空表单（不是错误，属于正常初始状态）
    translation.value = null
  }
}

async function save() {
  if (!current.value) return
  if (!title.value.trim()) {
    ElMessage.error('译文标题必填')
    return
  }
  saving.value = true
  try {
    await api.put(`/admin/v1/products/${current.value.id}/translations/${locale.value}`, {
      title: title.value.trim(),
      description: description.value.trim() || null
    })
    ElMessage.success('已保存草稿')
    await refreshTranslation(current.value.id)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function review() {
  if (!current.value) return
  saving.value = true
  try {
    await api.post(`/admin/v1/products/${current.value.id}/translations/${locale.value}/review`, {})
    ElMessage.success('已审核')
    await refreshTranslation(current.value.id)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '审核失败')
  } finally {
    saving.value = false
  }
}

async function publish() {
  if (!current.value) return
  saving.value = true
  try {
    await api.post(`/admin/v1/products/${current.value.id}/translations/${locale.value}/publish`, {})
    ElMessage.success('已发布')
    await refreshTranslation(current.value.id)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '发布失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1>商品译文</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="流程：录入草稿 → 审核 → 发布；发布后前台按语言读取生效"
              description="没有译文时前台回退中文标题，不会显示空白。" />

    <div class="mb-16 toolbar">
      <el-input v-model="keyword" placeholder="搜索商品标题" style="width: 260px" clearable />
      <el-button :loading="loading" @click="load">搜索</el-button>
    </div>

    <el-table :data="products" v-loading="loading" empty-text="没有商品">
      <el-table-column prop="title" label="商品" min-width="260" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑译文</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="`译文 · ${current?.title ?? ''}`" width="560px">
      <el-form label-width="90px">
        <el-form-item label="语言">
          <el-select v-model="locale" style="width: 160px" @change="current && refreshTranslation(current.id)">
            <el-option label="English" value="en" />
            <el-option label="日本語" value="ja" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-tag>{{ translation?.status ?? '未录入' }}</el-tag>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="title" data-testid="translation-title" placeholder="English title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="description" type="textarea" :rows="4" data-testid="translation-desc" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button :loading="saving" @click="save">保存草稿</el-button>
        <el-button :disabled="!translation || translation.status !== 'DRAFT'" @click="review">审核</el-button>
        <el-button type="primary" :disabled="!translation || translation.status !== 'REVIEWED'"
                   :loading="saving" @click="publish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.toolbar { display: flex; gap: 12px; }
</style>
