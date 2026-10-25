export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',
  devtools: { enabled: true },
  css: ['~/assets/main.css', '~/assets/themes.css'],
  // 开发/局域网联调：同源代理到后端，客户端无需知道 41280；生产用反向代理遵循同样路径
  runtimeConfig: {
    apiInternalBase: process.env.NUXT_API_INTERNAL_BASE || 'http://localhost:41280',
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://192.168.1.15:41280',
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