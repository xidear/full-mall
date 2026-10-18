<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  inventoryTrackingLabel,
  productStatusLabel,
  type Product,
  type ProductPage
} from '@fullmall/api-client'
import { api } from '../api'

const router = useRouter()
const products = ref<Product[]>([])
const loading = ref(false)
const error = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const page = await api.get<ProductPage>('/admin/v1/products', { locale: 'zh-CN', size: 100 })
    products.value = page.items
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function toggleStatus(product: Product) {
  const next = product.status === 'ACTIVE' ? 'DRAFT' : 'ACTIVE'
  try {
    await api.patch(`/admin/v1/products/${product.id}/status`, { status: next })
    ElMessage.success(next === 'ACTIVE' ? '商品已上架' : '商品已下架')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
  }
}

function price(product: Product) {
  const money = product.skus[0]?.price
  if (!money) return '-'
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: money.currency }).format(money.amount / 100)
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>商品管理</h2>
        <p class="muted">草稿、在售、下架状态统一管理；新建商品后默认为草稿。</p>
      </div>
      <el-button type="primary" @click="router.push('/products/new')">新建商品</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
    <el-table :data="products" stripe>
      <el-table-column label="图片" width="76">
        <template #default="{ row }">
          <img
            v-if="row.skus[0]?.imageUrl"
            :src="row.skus[0].imageUrl"
            :alt="row.title"
            class="product-thumb"
          >
          <span v-else class="product-thumb placeholder">FM</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="商品" min-width="220" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : row.status === 'DRAFT' ? 'info' : 'warning'">
            {{ productStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="库存粒度" width="140">
        <template #default="{ row }">{{ inventoryTrackingLabel(row.inventoryTrackingLevel) }}</template>
      </el-table-column>
      <el-table-column label="批次/效期" width="110">
        <template #default="{ row }">
          <el-tag :type="row.shelfLifeManaged ? 'warning' : 'info'">
            {{ row.shelfLifeManaged ? '启用' : '关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="规格数" width="90">
        <template #default="{ row }">{{ row.skus.length }}</template>
      </el-table-column>
      <el-table-column label="起售价" width="130">
        <template #default="{ row }">{{ price(row) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button
            link
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '下架' : '上架' }}
          </el-button>
          <el-button link type="primary" @click="router.push('/products/new')">新建相似</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !products.length" description="暂无商品，点击右上角新建" />
  </div>
</template>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.product-thumb {
  display: block;
  width: 44px;
  height: 44px;
  object-fit: cover;
  border-radius: 8px;
}

.product-thumb.placeholder {
  display: grid;
  place-items: center;
  background: #f0efe9;
  color: #8a8f8a;
  font-size: 12px;
  font-weight: 800;
}
</style>