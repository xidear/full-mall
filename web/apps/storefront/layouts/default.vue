<script setup lang="ts">
import { themePresets, toCssVars, type ThemeConfig } from '@fullmall/theme'
import type { Category } from '~/types'

const { count } = useCart()
const { token: memberToken } = useMember()
const { init: initLocale, t, locale, error: localeError } = useLocale()
await initLocale()
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
const mobileNavOpen = ref(false)

const siteUrl = computed(() => {
  const domain = (siteSettings.value?.site_domain || '').trim()
  if (!domain) return ''
  const port = domain.includes(':') ? domain.slice(domain.lastIndexOf(':') + 1) : ''
  const configured = siteSettings.value?.site_scheme
  const scheme = configured === 'http' || configured === 'https'
    ? configured
    : (!port || port === '443' ? 'https' : 'http')
  return `${scheme}://${domain}`
})
const icpUrl = computed(() => siteSettings.value?.icp_number ? 'https://beian.miit.gov.cn/' : '')
const policeUrl = computed(() => {
  const record = siteSettings.value?.public_security_record || ''
  const digits = (record.match(/\d{6,}/) || [])[0]
  return digits ? `https://beian.mps.gov.cn/#/query/webSearch?code=${digits}` : 'https://beian.mps.gov.cn/'
})
const licenseUrl = 'https://www.gsxt.gov.cn/index.html'
const telHref = computed(() => {
  const phone = siteSettings.value?.service_phone || ''
  return phone ? 'tel:' + phone.replace(/[^+\d]/g, '') : ''
})
const mailHref = computed(() => siteSettings.value?.service_email ? 'mailto:' + siteSettings.value.service_email : '')

useHead(() => ({
  htmlAttrs: { lang: locale.value, dir: locale.value === 'ar' ? 'rtl' : 'ltr' },
  link: siteUrl.value ? [{ rel: 'canonical', href: siteUrl.value }] : [],
  meta: siteUrl.value ? [{ property: 'og:url', content: siteUrl.value }] : []
}))

async function search() {
  await navigateTo({
    path: '/products',
    query: keyword.value.trim() ? { keyword: keyword.value.trim() } : {}
  })
}
</script>

<template>
  <div class="app-shell" :style="effectiveVars" :data-theme="effectiveTheme">
    <a href="#main-content" class="skip-link">跳到主内容</a>
    <div class="topbar">
      <div class="container topbar-inner">
        <span>{{ t('topbar.welcome') }}</span>
        <nav>
          <LocaleSwitcher class="locale-slot" />
          <NuxtLink v-if="!memberToken" to="/login">登录 / 注册</NuxtLink>
          <NuxtLink v-else to="/account">你好，会员</NuxtLink>
          <NuxtLink to="/favorites">{{ t('nav.favorites') }}</NuxtLink>
          <NuxtLink to="/history">{{ t('nav.history') }}</NuxtLink>
          <NuxtLink to="/notifications">{{ t('nav.notifications') }}</NuxtLink>
          <NuxtLink to="/legal/TERMS">{{ t('nav.help') }}</NuxtLink>
          <span v-if="runtime.appMode === 'demo'" class="demo-pill">只读演示</span>
        </nav>
      </div>
      <p v-if="localeError" class="container topbar-warning">语言包加载失败：{{ localeError }}</p>
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
          <label for="site-search" class="sr-only">搜索商品</label>
          <input
            id="site-search"
            v-model="keyword"
            type="search"
            :placeholder="t('search.placeholder')"
            :aria-label="t('search.button')"
            @keyup.enter="search"
          >
          <button type="button" @click="search">{{ t('search.button') }}</button>
        </div>

        <NuxtLink to="/cart" class="cart-link">
          {{ t('nav.cart') }}
          <span v-if="count" class="cart-count">{{ count }}</span>
        </NuxtLink>
      </div>

      <button
        type="button"
        class="menu-toggle"
        aria-label="打开商品分类菜单"
        :aria-expanded="mobileNavOpen"
        @click="mobileNavOpen = !mobileNavOpen"
      >
        ☰ 全部分类
      </button>
      <nav class="container category-nav" :class="{ open: mobileNavOpen }">
        <NuxtLink to="/products">{{ t('nav.allProducts') }}</NuxtLink>
        <NuxtLink
          v-for="category in categories ?? []"
          :key="category.id"
          :to="{ path: '/products', query: { categoryId: category.id } }"
        >
          {{ category.name }}
        </NuxtLink>
        <NuxtLink to="/promotions">{{ t('nav.promotions') }}</NuxtLink>
        <NuxtLink to="/legal/PICKUP">{{ t('nav.pickup') }}</NuxtLink>
      </nav>
    </header>

    <main id="main-content" class="container main-content">
      <div v-if="siteStatus !== 'OPEN'" class="maintenance-page">
        <p class="eyebrow">MAINTENANCE</p>
        <h1>{{ t('maintenance.title') }}</h1>
        <p>{{ siteSettings?.maintenance_message || t('maintenance.fallback') }}</p>
        <p v-if="siteSettings?.service_phone" class="muted">
          {{ t('footer.contact') }}：<a :href="telHref">{{ siteSettings.service_phone }}</a>
        </p>
      </div>
      <slot v-else />
    </main>

    <footer class="site-footer">
      <div class="container footer-service">
        <NuxtLink to="/legal/TERMS"><strong>{{ t('footer.auth.title') }}</strong><span>{{ t('footer.auth.desc') }}</span></NuxtLink>
        <NuxtLink to="/legal/REFUND"><strong>{{ t('footer.refund.title') }}</strong><span>{{ t('footer.refund.desc') }}</span></NuxtLink>
        <NuxtLink to="/legal/PICKUP"><strong>{{ t('footer.pickup.title') }}</strong><span>{{ t('footer.pickup.desc') }}</span></NuxtLink>
        <NuxtLink to="/legal/REFUND"><strong>{{ t('footer.returns.title') }}</strong><span>{{ t('footer.returns.desc') }}</span></NuxtLink>
      </div>

      <nav class="container footer-links" aria-label="法律与帮助">
        <NuxtLink to="/legal/TERMS">{{ t('footer.legal.terms') }}</NuxtLink>
        <NuxtLink to="/legal/PRIVACY">{{ t('footer.legal.privacy') }}</NuxtLink>
        <NuxtLink to="/legal/REFUND">{{ t('footer.legal.refund') }}</NuxtLink>
        <NuxtLink to="/legal/PICKUP">{{ t('footer.legal.pickup') }}</NuxtLink>
        <NuxtLink to="/account">{{ t('nav.account') }}</NuxtLink>
        <NuxtLink to="/notifications">{{ t('nav.notifications') }}</NuxtLink>
      </nav>

      <div class="container footer-inner">
        <span>
          {{ siteSettings?.company_name || 'Full Mall' }}
          <template v-if="siteSettings?.icp_number">
            · <a :href="icpUrl" target="_blank" rel="noopener noreferrer">{{ siteSettings.icp_number }}</a>
          </template>
        </span>
        <span>
          <template v-if="siteSettings?.public_security_record">
            <a :href="policeUrl" target="_blank" rel="noopener noreferrer">{{ siteSettings.public_security_record }}</a>
          </template>
          <template v-if="siteSettings?.business_license_no">
            · <a :href="licenseUrl" target="_blank" rel="noopener noreferrer">{{ t('footer.license') }}：{{ siteSettings.business_license_no }}</a>
          </template>
        </span>
        <span v-if="siteSettings?.service_phone || siteSettings?.service_email">
          {{ t('footer.contact') }}：
          <a v-if="siteSettings?.service_phone" :href="telHref">{{ siteSettings.service_phone }}</a>
          <template v-if="siteSettings?.service_phone && siteSettings?.service_email"> · </template>
          <a v-if="siteSettings?.service_email" :href="mailHref">{{ siteSettings.service_email }}</a>
        </span>
      </div>
      <div class="container footer-bottom">
        <span>Full Mall · 开源跨境商城</span>
        <span>{{ t('footer.note') }}</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.skip-link {
  position: absolute;
  left: -9999px;
  top: 0;
  z-index: 1000;
  padding: 8px 14px;
  background: #fff;
  color: #111;
  border: 1px solid var(--brand, #e1251b);
  border-radius: 0 0 8px 0;
}
.skip-link:focus { left: 0; }
.topbar-warning { margin: 0; padding-bottom: 6px; color: #b45309; font-size: 12px; }
.footer-links { display: flex; flex-wrap: wrap; gap: 18px; padding: 14px 0 8px; border-top: 1px solid rgba(0, 0, 0, .06); }
.footer-links a { color: var(--muted, #666); font-size: 13px; }
.footer-links a:hover { color: var(--brand, #e1251b); }
.menu-toggle { display: none; }
@media (max-width: 768px) {
  .menu-toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 44px;
    margin: 8px 16px 0;
    padding: 0 14px;
    border: 1px solid var(--line, #ddd);
    border-radius: 999px;
    background: #fff;
    color: var(--text, #222);
    font-size: 14px;
  }
  .category-nav { display: none; }
  .category-nav.open { display: flex; flex-wrap: wrap; }
}
@media (max-width: 768px) {
  .topbar-inner nav a,
  .topbar-inner nav :deep(.locale-switcher select),
  .category-nav a {
    min-height: 44px;
    min-width: 44px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0 10px;
  }
  .search-box button { min-height: 44px; }
  .category-nav {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: thin;
  }
  .footer-inner { gap: 10px; }
}
</style>
