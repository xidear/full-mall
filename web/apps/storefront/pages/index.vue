<script setup lang="ts">
const { locale, t } = useLocale()
const categoryLabel = useCategoryLabel()
import type { Category, Product, ProductPage } from '~/types'

const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
// 首页一次取 50 条给各区块（降价/生鲜/工业品）切片复用，避免每个区块各发一次请求。
// 代价：商品超过 50 个时首页只覆盖前 50 条——首页是“精选”语义，完整列表在 /products。
const { data, error } = await useFetch<ProductPage>(`${base}/api/v1/products`, {
  key: 'home-products',
  query: { locale: locale.value, page: 0, size: 50 }
})
const { data: categories } = await useAsyncData<Category[]>('categories', () =>
  $fetch<Category[]>(`${base}/api/v1/categories`)
)

// 加载失败必须抛 502 交给 error.vue，而不是渲染一个“看起来正常”的空首页：
// 空数组 ≠ 请求失败（AGENTS.md §1），否则顾客只会以为这家店没货。
if (error.value) {
  console.error('[home] 商品加载失败:', error.value)
  throw createError({
    statusCode: 502,
    statusMessage: '商品加载失败，请稍后重试'
  })
}

const products = computed(() => data.value?.items ?? [])
const { siteName, siteTagline } = useSite()
useHead(() => ({ title: siteTagline.value ? `${siteName.value} · ${siteTagline.value}` : siteName.value }))
const deals = computed(() => products.value.filter(product => product.skus[0]?.compareAtPrice).slice(0, 4))
const shelfLife = computed(() => products.value.filter(product => product.shelfLifeManaged).slice(0, 4))
// 工业品专区靠标题硬匹配，是演示数据的权宜之计：后端没有“工业品”标记，
// 新上架商品不会自动进来。等后端提供字段后应改为按标记筛选。
const industrial = computed(() =>
  products.value.filter(product => ['工业级步进电机', '不锈钢法兰盘'].includes(product.title)).slice(0, 4)
)
</script>

<template>
  <section class="hero">
    <img class="hero-art" src="/images/hero.svg" alt="">
    <div class="hero-main">
      <p class="eyebrow">{{ t('home.eyebrow') }}</p>
      <h1>{{ t('home.title') }}</h1>
      <p class="hero-copy">
        {{ t('home.copy') }}
      </p>
      <div class="hero-actions">
        <NuxtLink to="/products" class="button">{{ t('home.shop') }}</NuxtLink>
        <NuxtLink to="/legal/PICKUP" class="button button-ghost">{{ t('home.pickup') }}</NuxtLink>
      </div>
      <div class="hero-stats">
        <span><strong>2 小时</strong>{{ t('home.stat.pickup') }}</span>
        <span><strong>7 天</strong>{{ t('home.stat.returns') }}</span>
        <span><strong>100%</strong>{{ t('home.stat.auth') }}</span>
      </div>
    </div>
    <div class="hero-side">
      <div class="hero-side-card hot">
        <span>今日爆款</span>
        <strong>限时直降 100 元</strong>
        <NuxtLink to="/products">去看看 →</NuxtLink>
      </div>
      <div class="hero-side-card">
        <span>企业采购</span>
        <strong>工业品专区</strong>
        <NuxtLink to="/products?keyword=工业">选型询价 →</NuxtLink>
      </div>
    </div>
  </section>

  <section class="category-strip">
    <NuxtLink
      v-for="category in categories ?? []"
      :key="category.id"
      :to="{ path: '/products', query: { category: category.code } }"
      class="category-tile"
    >
      <span class="category-icon">{{ categoryLabel(category).slice(0, 1) }}</span>
      <strong>{{ categoryLabel(category) }}</strong>
      <small>精选好物</small>
    </NuxtLink>
  </section>

  <section class="section">
    <div class="section-heading">
      <div>
        <p class="eyebrow">限时好价</p>
        <h2>{{ t('home.deals') }}</h2>
      </div>
      <NuxtLink to="/products" class="section-more">{{ t('home.dealsMore') }}</NuxtLink>
    </div>
    <div class="product-grid">
      <ProductCard v-for="product in deals" :key="product.id" :product="product" />
    </div>
  </section>

  <section class="section">
    <div class="section-heading">
      <div>
        <p class="eyebrow">热卖推荐</p>
        <h2>{{ t('home.hot') }}</h2>
      </div>
      <NuxtLink to="/products" class="section-more">{{ t('home.hotMore') }}</NuxtLink>
    </div>
    <div class="product-grid">
      <ProductCard v-for="product in products.slice(0, 8)" :key="product.id" :product="product" />
    </div>
  </section>

  <section class="section two-columns">
    <div>
      <div class="section-heading">
        <div>
          <p class="eyebrow">新鲜直达</p>
          <h2>{{ t('home.fresh') }}</h2>
        </div>
      </div>
      <div class="product-grid compact">
        <ProductCard v-for="product in shelfLife" :key="product.id" :product="product" />
      </div>
    </div>
    <div class="industrial-panel">
      <p class="eyebrow">企业采购</p>
      <h2>{{ t('home.industrial') }}</h2>
      <p>{{ t('home.industrialCopy') }}</p>
      <div class="industrial-list">
        <NuxtLink v-for="product in industrial" :key="product.id" :to="`/product/${product.id}`">
          {{ product.title }} <span>{{ formatMoney(product.skus[0]?.price) }}</span>
        </NuxtLink>
      </div>
      <NuxtLink to="/products?keyword=工业" class="button">{{ t('home.industrialCta') }}</NuxtLink>
    </div>
  </section>

  <p v-if="!products.length" class="empty-state">{{ t('home.empty') }}</p>
</template>