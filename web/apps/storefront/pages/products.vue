<script setup lang="ts">
import type { Category, Product, ProductPage } from '~/types'

const { locale, t } = useLocale()
const { siteName } = useSite()
const categoryLabel = useCategoryLabel()
const route = useRoute()
const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
// 对外 URL 用分类 code（/products?category=beauty）；旧的 categoryId 查询参数继续兼容。
const categoryCode = computed(() => typeof route.query.category === 'string' ? route.query.category : '')
const legacyCategoryId = computed(() => typeof route.query.categoryId === 'string' ? route.query.categoryId : '')
const keyword = computed(() => typeof route.query.keyword === 'string' ? route.query.keyword : undefined)
// 页大小必须与 loadMore 的推算一致：下一页由“已加载条数 / PAGE_SIZE”算，
// 两处不同步会重复或漏掉一页（表现为商品重复出现或莫名少几件）。
const PAGE_SIZE = 20
// 服务端排序（白名单在 API 层）；顾客端只选择排序方式，不做前端伪排序
const sortKey = ref<'createdAt' | 'minPrice'>('createdAt')
const direction = ref<'asc' | 'desc'>('desc')
const extraItems = ref<Product[]>([])
const loadingMore = ref(false)
const loadMoreError = ref('')

const { data: categories } = await useAsyncData<Category[]>('products-categories', () =>
  $fetch<Category[]>(`${base}/api/v1/categories`)
)
const activeCategory = computed(() => categories.value?.find(category =>
  categoryCode.value ? category.code === categoryCode.value : category.id === legacyCategoryId.value
))
const categoryId = computed(() => activeCategory.value?.id)

// 依赖项放进 watch：筛选/排序一变就重取第一页，URL 里的条件与渲染结果始终对应。
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
  { watch: [sortKey, direction, categoryCode, legacyCategoryId, keyword] }
)

if (error.value) {
  console.error('[products] 商品加载失败:', error.value)
  throw createError({ statusCode: 502, statusMessage: '商品加载失败，请稍后重试' })
}

// 排序或筛选变化时，清掉已追加的页
// 不清空的话，旧筛选结果会和新结果混在同一网格里，
// 顾客看到的是“筛过了却还留着别的分类”的错觉。
watch([sortKey, direction, categoryCode, legacyCategoryId, keyword], () => { extraItems.value = [] })

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
    // 加载更多失败只在底部提示、不清空已有列表：局部失败 ≠ 整体失败，
    // 文案走 apiErrorMessage，不把状态码/路径暴露给顾客。
  } catch (cause) {
    loadMoreError.value = apiErrorMessage(cause, '加载更多失败，请稍后重试')
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

const activeCategoryName = computed(() =>
  activeCategory.value ? categoryLabel(activeCategory.value) : '')
useHead(() => ({ title: `${activeCategoryName.value || (keyword.value ? '搜索：' + keyword.value : '全部商品')} · ${siteName.value}` }))

</script>

<template>
  <section class="products-page">
    <aside class="filter-sidebar">
      <div class="filter-block">
        <h3>商品分类</h3>
        <NuxtLink to="/products" class="filter-link" :class="{ active: !categoryCode && !legacyCategoryId }">全部商品</NuxtLink>
        <NuxtLink
          v-for="category in categories ?? []"
          :key="category.id"
          :to="{ path: '/products', query: { category: category.code } }"
          class="filter-link"
          :class="{ active: activeCategory?.id === category.id }"
        >
          {{ categoryLabel(category) }}
        </NuxtLink>
      </div>
      <div class="filter-block">
        <h3>服务与政策</h3>
        <NuxtLink to="/legal/PICKUP" class="filter-note">门店自提</NuxtLink>
        <NuxtLink to="/legal/REFUND" class="filter-note">七天无理由</NuxtLink>
        <NuxtLink to="/legal/TERMS" class="filter-note">服务条款</NuxtLink>
      </div>
    </aside>

    <div class="products-main">
      <div class="products-head">
        <div>
          <p class="eyebrow">商品列表</p>
          <h1>{{ activeCategoryName || (keyword ? `搜索：${keyword}` : '全部商品') }}</h1>
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
      <EmptyState
        v-if="!products.length && !pending"
        icon="search"
        title="没有找到符合条件的商品"
        :description="keyword ? `换个关键词试试，或清空「${keyword}」筛选。` : '当前分类暂时没有商品，看看全部商品吧。'"
      >
        <NuxtLink to="/products" class="button button-secondary">{{ keyword ? '清空搜索' : '查看全部商品' }}</NuxtLink>
      </EmptyState>

      <div v-if="hasMore" class="load-more">
        <button type="button" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
        <p v-if="loadMoreError" class="load-more-error">{{ loadMoreError }}</p>
      </div>
      <ListEnd
        v-else-if="products.length"
        :count="products.length"
        :noun="t('list.end.noun')"
      />
    </div>
  </section>
</template>

<style scoped>
.loading-hint {
  color: var(--muted);
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
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  cursor: pointer;
}

.load-more button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.load-more-error {
  color: var(--danger);
  font-size: 13px;
}
</style>
