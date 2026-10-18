<script setup lang="ts">
import type { Category, Product, ProductPage } from '~/types'

const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const { data, error } = await useFetch<ProductPage>(`${base}/api/v1/products`, {
  query: { locale: 'zh-CN', page: 0, size: 50 }
})
const { data: categories } = await useAsyncData<Category[]>('categories', () =>
  $fetch<Category[]>(`${base}/api/v1/categories`)
)

if (error.value) {
  throw createError({
    statusCode: 502,
    statusMessage: `商品加载失败：${error.value.message}`
  })
}

const products = computed(() => data.value?.items ?? [])
const deals = computed(() => products.value.filter(product => product.skus[0]?.compareAtPrice).slice(0, 4))
const shelfLife = computed(() => products.value.filter(product => product.shelfLifeManaged).slice(0, 4))
const industrial = computed(() =>
  products.value.filter(product => ['工业级步进电机', '不锈钢法兰盘'].includes(product.title)).slice(0, 4)
)
</script>

<template>
  <section class="hero">
    <img class="hero-art" src="/images/hero.svg" alt="">
    <div class="hero-main">
      <p class="eyebrow">FULL MALL · 品质生活</p>
      <h1>好物直达，门店自提更快</h1>
      <p class="hero-copy">
        覆盖生鲜、数码、家居、美妆、运动和工业品。支持多仓发货、门店自提、供应商代发与批次效期管理。
      </p>
      <div class="hero-actions">
        <NuxtLink to="/products" class="button">立即选购</NuxtLink>
        <NuxtLink to="/legal/PICKUP" class="button button-ghost">查看自提规则</NuxtLink>
      </div>
      <div class="hero-stats">
        <span><strong>2 小时</strong>门店自提</span>
        <span><strong>7 天</strong>无理由退货</span>
        <span><strong>100%</strong>正品保障</span>
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
      :to="{ path: '/products', query: { categoryId: category.id } }"
      class="category-tile"
    >
      <span class="category-icon">{{ category.name.slice(0, 1) }}</span>
      <strong>{{ category.name }}</strong>
      <small>精选好物</small>
    </NuxtLink>
  </section>

  <section class="section">
    <div class="section-heading">
      <div>
        <p class="eyebrow">限时好价</p>
        <h2>今日特惠</h2>
      </div>
      <NuxtLink to="/products" class="section-more">查看全部 →</NuxtLink>
    </div>
    <div class="product-grid">
      <ProductCard v-for="product in deals" :key="product.id" :product="product" />
    </div>
  </section>

  <section class="section">
    <div class="section-heading">
      <div>
        <p class="eyebrow">热卖推荐</p>
        <h2>大家都在买</h2>
      </div>
      <NuxtLink to="/products" class="section-more">更多商品 →</NuxtLink>
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
          <h2>生鲜与批次效期</h2>
        </div>
      </div>
      <div class="product-grid compact">
        <ProductCard v-for="product in shelfLife" :key="product.id" :product="product" />
      </div>
    </div>
    <div class="industrial-panel">
      <p class="eyebrow">企业采购</p>
      <h2>工业品与设备选型</h2>
      <p>步进电机、法兰、标准件，支持批量采购和供应商直发。</p>
      <div class="industrial-list">
        <NuxtLink v-for="product in industrial" :key="product.id" :to="`/product/${product.id}`">
          {{ product.title }} <span>{{ formatMoney(product.skus[0]?.price) }}</span>
        </NuxtLink>
      </div>
      <NuxtLink to="/products?keyword=工业" class="button">进入工业品专区</NuxtLink>
    </div>
  </section>

  <p v-if="!products.length" class="empty-state">暂无商品，请确认后端已启动并完成 seed。</p>
</template>