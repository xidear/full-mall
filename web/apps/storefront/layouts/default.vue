<script setup lang="ts">
import { themePresets, toCssVars, type ThemeConfig } from '@fullmall/theme'
import type { Category } from '~/types'

const { count } = useCart()
const runtimeConfig = useRuntimeConfig()
const { public: runtime } = runtimeConfig
const { theme, themeVars } = useTheme()
const base = import.meta.server ? runtimeConfig.apiInternalBase : runtimeConfig.public.apiBase
const route = useRoute()
const { data: savedTheme } = await useAsyncData<ThemeConfig | null>('pc-theme-config', async () => {
  const response = await $fetch<{ config: ThemeConfig | null }>(`${base}/api/v1/theme`, {
    query: { channel: 'PC', market: 'CN' }
  })
  return response.config
})
const effectiveVars = computed(() => {
  if (route.query.theme) {
    return themeVars.value
  }
  return savedTheme.value ? toCssVars(savedTheme.value) : themeVars.value
})
const effectiveTheme = computed(() => {
  if (route.query.theme) {
    return theme.value
  }
  return (savedTheme.value?.preset as typeof theme.value) || theme.value
})
const { data: categories } = await useAsyncData('categories', () =>
  $fetch<Category[]>(`${base}/api/v1/categories`)
)
const { data: siteSettings } = await useAsyncData<Record<string, string>>('site-settings', () =>
  $fetch<Record<string, string>>(`${base}/api/v1/site/settings`)
)
const siteStatus = computed(() => siteSettings.value?.site_status || 'OPEN')
const keyword = ref('')

async function search() {
  await navigateTo({
    path: '/products',
    query: keyword.value.trim() ? { keyword: keyword.value.trim() } : {}
  })
}
</script>

<template>
  <div class="app-shell" :style="effectiveVars" :data-theme="effectiveTheme">
    <div class="topbar">
      <div class="container topbar-inner">
        <span>欢迎来到 Full Mall，正品保障 · 门店自提 · 七天无理由</span>
        <nav>
          <NuxtLink to="/account">我的订单</NuxtLink>
          <NuxtLink to="/legal/TERMS">帮助中心</NuxtLink>
          <span v-if="runtime.appMode === 'demo'" class="demo-pill">只读演示</span>
        </nav>
      </div>
    </div>

    <header class="site-header">
      <div class="container header-main">
        <NuxtLink to="/" class="brand">
          <span class="brand-mark">FM</span>
          <span>
            <strong>Full Mall</strong>
            <small>全品类商城</small>
          </span>
        </NuxtLink>

        <div class="search-box">
          <input
            v-model="keyword"
            type="search"
            placeholder="搜索商品、品牌、工业型号"
            @keyup.enter="search"
          >
          <button type="button" @click="search">搜索</button>
        </div>

        <NuxtLink to="/cart" class="cart-link">
          购物车
          <span v-if="count" class="cart-count">{{ count }}</span>
        </NuxtLink>
      </div>

      <nav class="container category-nav">
        <NuxtLink to="/products">全部商品</NuxtLink>
        <NuxtLink
          v-for="category in categories ?? []"
          :key="category.id"
          :to="{ path: '/products', query: { categoryId: category.id } }"
        >
          {{ category.name }}
        </NuxtLink>
        <NuxtLink to="/legal/PICKUP">门店自提</NuxtLink>
      </nav>
    </header>

    <main class="container main-content">
      <div v-if="siteStatus !== 'OPEN'" class="maintenance-page">
        <p class="eyebrow">MAINTENANCE</p>
        <h1>系统维护中</h1>
        <p>{{ siteSettings?.maintenance_message || '系统维护中，请稍后再试。' }}</p>
        <p class="muted">客服电话：{{ siteSettings?.service_phone || '400-000-0000' }}</p>
      </div>
      <slot v-else />
    </main>

    <footer class="site-footer">
      <div class="container footer-service">
        <div><strong>正品保障</strong><span>品牌直供，品质可追溯</span></div>
        <div><strong>极速退款</strong><span>售后审核通过后原路退回</span></div>
        <div><strong>门店自提</strong><span>就近门店，扫码核销</span></div>
        <div><strong>七天无理由</strong><span>符合条件的商品支持</span></div>
      </div>
      <div class="container footer-inner">
        <span>{{ siteSettings?.company_name || 'Full Mall' }} · {{ siteSettings?.icp_number || '' }}</span>
        <span>{{ siteSettings?.public_security_record || '' }} · 客服 {{ siteSettings?.service_phone || '' }} · {{ siteSettings?.service_email || '' }}</span>
      </div>
      <div class="container footer-bottom">
        <span>Full Mall · 开源跨境商城</span>
        <span>金额单位：分 · 演示数据不产生真实交易</span>
      </div>
    </footer>
  </div>
</template>