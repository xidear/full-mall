export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',
  devtools: { enabled: true },
  css: ['~/assets/main.css', '~/assets/themes.css'],
  runtimeConfig: {
    apiInternalBase: process.env.NUXT_API_INTERNAL_BASE || 'http://localhost:8080',
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080',
      appMode: process.env.NUXT_PUBLIC_APP_MODE || 'dev',
      theme: process.env.NUXT_PUBLIC_THEME || 'mall'
    }
  },
  app: {
    head: {
      title: 'Full Mall',
      htmlAttrs: { lang: 'zh-CN' },
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: '源码开放跨境商城演示站' }
      ]
    }
  }
})