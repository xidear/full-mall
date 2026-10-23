<script setup lang="ts">
const { locale } = useLocale()
import type { Category, Product, ProductPage } from '~/types'

const route = useRoute()
const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const categoryId = computed(() => typeof route.query.categoryId === 'string' ? route.query.categoryId : undefined)
const keyword = computed(() => typeof route.query.keyword === 'string' ? route.query.keyword : undefined)
const PAGE_SIZE = 20
// 服务端排序（白名单在 API 层）；顾客端只选择排序方式，不做前端伪排序
const sortKey = ref<'createdAt' | 'minPrice'>('createdAt')
const direction = ref<'asc' | 'desc'>('desc')
const extraItems = ref<Product[]>([])
const loadingMore = ref(false)
const loadMoreError = ref('')

const { data, pending, error, refresh } = await useAsyncData<ProductPage>(
  'products',
  () => $fetch<ProductPage>(`${base}/api/v1/products`, {
    query: {
      locale: locale.value,
      categoryId: categoryId.value,
      keyword: keyword.value,
      page: 0,
      size: PAGE_SIZE,
      sort: sortKey.value,
      direction: direction.value
    }
  }),
  { watch: [sortKey, direction, categoryId, keyword] }
)

if (error.value) {
  throw createError({ statusCode: 502, statusMessage: `商品加载失败：${error.value.message}` })
}

// 排序或筛选变化时，清掉已追加的页
watch([sortKey, direction, categoryId, keyword], () => { extraItems.value = [] })

// K7 埋点：搜索词（用于热搜词与选品）
watch(keyword, (value) => {
  if (value && value.trim()) {
    track('SEARCH', { keyword: value.trim() })
  }
}, { immediate: true })

const products = computed(() => [...(data.value?.items ?? []), ...extraItems.value])
const total = computed(() => data.value?.total ?? products.value.length)
const hasMore = computed(() => products.value.length < total.value)

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  loadMoreError.value = ''
  try {
    const next = Math.floor(products.value.length / PAGE_SIZE)
    const page = await $fetch<ProductPage>(`${base}/api/v1/products`, {
      query: {
        locale: locale.value, categoryId: categoryId.value, keyword: keyword.value,
        page: next, size: PAGE_SIZE, sort: sortKey.value, direction: direction.value
      }
    })
    extraItems.value = [...extraItems.value, ...page.items]
  } catch (cause) {
    loadMoreError.value = cause instanceof Error ? cause.message : '加载更多失败'
  } finally {
    loadingMore.value = false
  }
}

function togglePriceSort() {
  if (sortKey.value !== 'minPrice') {
    sortKey.value = 'minPrice'
    direction.value = 'asc'
    return
  }
  direction.value = direction.value === 'asc' ? 'desc' : 'asc'
}

const activeCategory = computed(() =>
  categories.value?.find(category => category.id === categoryId.value)
)
</script>

<template>
  <section class="products-page">
    <aside class="filter-sidebar">
      <div class="filter-block">
        <h3>商品分类</h3>
        <NuxtLink to="/products" class="filter-link" :class="{ active: !categoryId }">全部商品</NuxtLink>
        <NuxtLink
          v-for="category in categories ?? []"
          :key="category.id"
          :to="{ path: '/products', query: { categoryId: category.id } }"
          class="filter-link"
          :class="{ active: categoryId === category.id }"
        >
          {{ category.name }}
        </NuxtLink>
      </div>
      <div class="filter-block">
        <h3>服务</h3>
        <span class="filter-note">门店自提</span>
        <span class="filter-note">七天无理由</span>
        <span class="filter-note">批次效期</span>
      </div>
    </aside>

    <div class="products-main">
      <div class="products-head">
        <div>
          <p class="eyebrow">商品列表</p>
          <h1>{{ activeCategory?.name || (keyword ? `搜索：${keyword}` : '全部商品') }}</h1>
          <p class="muted">共 {{ total }} 件商品，已显示 {{ products.length }} 件</p>
        </div>
        <button type="button" class="sort-select" :disabled="pending" @click="togglePriceSort">
          价格{{ sortKey === 'minPrice' ? (direction === 'asc' ? ' ↑' : ' ↓') : '排序' }}
        </button>
      </div>

      <p v-if="pending" class="loading-hint">正在加载…</p>
      <div class="product-grid">
        <ProductCard v-for="product in products" :key="product.id" :product="product" />
      </div>
      <p v-if="!products.length && !pending" class="empty-state">没有找到符合条件的商品</p>

      <div v-if="hasMore" class="load-more">
        <button type="button" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
        <p v-if="loadMoreError" class="load-more-error">{{ loadMoreError }}</p>
      </div>
      <p v-else-if="products.length" class="empty-state">已经到底了</p>
    </div>
  </section>
</template>

<style scoped>
.loading-hint {
  color: #6b7280;
  padding: 8px 0;
}

.load-more {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin: 24px 0 8px;
}

.load-more button {
  padding: 10px 24px;
  border: 1px solid #d1d5db;
  border-radius: 999px;
  background: #fff;
  cursor: pointer;
}

.load-more button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.load-more-error {
  color: #dc2626;
  font-size: 13px;
}
</style>
