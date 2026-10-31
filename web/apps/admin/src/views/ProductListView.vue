<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  formatMoney,
  inventoryTrackingLabel,
  productStatusLabel,
  type Product,
  type ProductPage
} from '@fullmall/api-client'
import { api } from '../api'
import AdminThumb from '../components/AdminThumb.vue'
import SkuPriceDialog from '../components/SkuPriceDialog.vue'

const router = useRouter()
const products = ref<Product[]>([])
const loading = ref(false)
const error = ref('')

const page = ref(0)
const size = ref(20)
const total = ref(0)
// 正在切换状态的商品 ID：避免重复点击导致上下架抖动
const actionId = ref('')
// 规格改价弹窗：作用域是"当前这一行商品"
const priceDialog = ref(false)
const priceProduct = ref<Product | null>(null)

function openPriceDialog(product: Product) {
  priceProduct.value = product
  priceDialog.value = true
}

onMounted(load)

const sortKey = ref('createdAt')
const sortDirection = ref<'asc' | 'desc'>('desc')

// 服务端排序（本页数据无法全局排序），且必须同时 page=0：停在第 5 页换排序会落到新结果的第 5 页，
// 看起来像“数据没了”。
function onSortChange(payload: { prop: string | null; order: 'ascending' | 'descending' | null }) {
  sortKey.value = payload.prop ?? 'createdAt'
  sortDirection.value = payload.order === 'ascending' ? 'asc' : 'desc'
  page.value = 0
  load()
}

function changePage(next: number) {
  page.value = next - 1
  load()
}

// 商品到几百款时"翻页找"不可用；后端一直支持 keyword，前端之前没接
const keyword = ref('')
let keywordTimer: ReturnType<typeof setTimeout> | undefined
function onKeywordInput() {
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => { page.value = 0; void load() }, 350)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get<ProductPage>('/admin/v1/products',
      { locale: 'zh-CN', page: page.value, size: size.value,
        keyword: keyword.value.trim() || undefined,
        sort: sortKey.value, direction: sortDirection.value })
    products.value = response.items
    // total 兜底用本页长度：后端正常会返回 total，这个兜底只影响分页器显示，
    // 却会掩盖“后端漏返回 total”的契约问题——排查分页异常先看这里拿到的值。
    total.value = response.total ?? response.items.length
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function toggleStatus(product: Product) {
  if (actionId.value) return
  const next = product.status === 'ACTIVE' ? 'DRAFT' : 'ACTIVE'
  actionId.value = product.id
  try {
    await api.patch(`/admin/v1/products/${product.id}/status`, { status: next })
    ElMessage.success(next === 'ACTIVE' ? '商品已上架' : '商品已下架')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
  } finally {
    actionId.value = ''
  }
}

// 起售价取第一个 SKU（列表不做最低价聚合，那要后端算）；没有 SKU 返回 "-" 而不是 0，
// 0 会被误读成“免费”。金额按币种小数位换算，禁止写死 /100。
function price(product: Product) {
  const money = product.skus[0]?.price
  if (!money) return '-'
  return formatMoney(money)
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>商品管理</h2>
        <p class="muted">草稿、在售、下架状态统一管理；新建商品后默认为草稿。</p>
      </div>
      <div class="header-actions">
        <el-input v-model="keyword" placeholder="搜索商品标题 / SKU 编码" clearable style="width: 260px"
                  @input="onKeywordInput" />
        <el-button type="primary" @click="router.push('/products/new')">新建商品</el-button>
      </div>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
    <el-table :data="products" stripe @sort-change="onSortChange">
      <el-table-column label="图片" width="76">
        <template #default="{ row }">
          <!-- 统一缩略图组件：地址为空或加载失败都回落到占位块，不再出现裂图 + 文案溢出 -->
          <AdminThumb :src="row.skus[0]?.imageUrl" :alt="row.title" />
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
      <el-table-column label="操作" width="330">
        <template #default="{ row }">
          <el-button
            link
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            :loading="actionId === row.id"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '下架' : '上架' }}
          </el-button>
          <el-button link type="primary" @click="router.push(`/products/${row.id}/edit`)">编辑</el-button>
          <el-button link type="primary" @click="openPriceDialog(row)">规格改价</el-button>
          <el-button link type="primary" @click="router.push({ path: `/products/${row.id}/edit`, query: { tab: 'translations' } })">译文</el-button>
          <el-button link @click="router.push('/products/new')">新建相似</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-footer">
      <span class="muted">共 {{ total }} 个商品</span>
      <el-pagination
        layout="prev, pager, next"
        :page-size="size"
        :total="total"
        :current-page="page + 1"
        @current-change="changePage" />
    </div>
    <el-empty v-if="!loading && !products.length" description="暂无商品，点击右上角新建" />

    <SkuPriceDialog
      v-model="priceDialog"
      :product-id="priceProduct?.id ?? ''"
      :product-title="priceProduct?.title"
      @applied="load"
    />
  </div>
</template>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>