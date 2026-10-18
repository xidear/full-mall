<script setup lang="ts">
import type { Category, Product, ProductPage } from '~/types'

const route = useRoute()
const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const categoryId = computed(() => typeof route.query.categoryId === 'string' ? route.query.categoryId : undefined)
const keyword = computed(() => typeof route.query.keyword === 'string' ? route.query.keyword : undefined)
const sort = ref<'default' | 'priceAsc' | 'priceDesc'>('default')

const { data, error } = await useFetch<ProductPage>(() => `${base}/api/v1/products`, {
  query: {
    locale: 'zh-CN',
    categoryId: categoryId.value,
    keyword: keyword.value,
    page: 0,
    size: 100
  }
})
const { data: categories } = await useAsyncData<Category[]>('categories', () =>
  $fetch<Category[]>(`${base}/api/v1/categories`)
)

if (error.value) {
  throw createError({ statusCode: 502, statusMessage: `商品加载失败：${error.value.message}` })
}

const products = computed(() => {
  const items = [...(data.value?.items ?? [])]
  if (sort.value === 'priceAsc') {
    items.sort((a, b) => (a.skus[0]?.price.amount ?? 0) - (b.skus[0]?.price.amount ?? 0))
  } else if (sort.value === 'priceDesc') {
    items.sort((a, b) => (b.skus[0]?.price.amount ?? 0) - (a.skus[0]?.price.amount ?? 0))
  }
  return items
})

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
          <p class="muted">共 {{ data?.total ?? 0 }} 件商品</p>
        </div>
        <select v-model="sort" class="sort-select">
          <option value="default">综合排序</option>
          <option value="priceAsc">价格从低到高</option>
          <option value="priceDesc">价格从高到低</option>
        </select>
      </div>

      <div class="product-grid">
        <ProductCard v-for="product in products" :key="product.id" :product="product" />
      </div>
      <p v-if="!products.length" class="empty-state">没有找到符合条件的商品</p>
    </div>
  </section>
</template>