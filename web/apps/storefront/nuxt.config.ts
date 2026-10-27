export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',
  devtools: { enabled: true },
  css: ['~/assets/main.css', '~/assets/themes.css'],
  // 开发/局域网联调：同源代理到后端，客户端无需知道 41280；生产用反向代理遵循同样路径
  runtimeConfig: {
    apiInternalBase: process.env.NUXT_API_INTERNAL_BASE || 'http://localhost:41280',
    public: {
      // 同源相对路径：客户端经 Nitro/edge 代理到 API，避免 CORS、混合内容与三地址行为不一致
      apiBase: process.env.NUXT_PUBLIC_API_BASE || '',
      appMode: process.env.NUXT_PUBLIC_APP_MODE || 'dev',
      theme: process.env.NUXT_PUBLIC_THEME || 'mall'
    }
  },
  vite: {
    server: {
      allowedHosts: ['company.binnarui.com', '192.168.1.15', 'localhost']
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