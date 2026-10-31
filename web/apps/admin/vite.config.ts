import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 后端地址不写死：dev 由下面 proxy 转发到 41280，生产由 nginx / vite preview 转发，三处必须同步改。
// 漏配前缀时请求会被 history 回落当成前端路由、返回 index.html，前端拿 HTML 当 JSON 解析，报 "Unexpected token '<'"。
const backendProxy = {
  '^/api/': 'http://localhost:41280',
  '^/admin/': 'http://localhost:41280',
  '^/store/': 'http://localhost:41280',
  '^/supplier/': 'http://localhost:41280',
  '^/open/': 'http://localhost:41280',
  '^/mock/': 'http://localhost:41280',
  '^/images/': 'http://localhost:41280',
  '^/assets/': 'http://localhost:41280',
  '^/media/': 'http://localhost:41280'
}

export default defineConfig({
  plugins: [vue()],
  // 构建产物放 /ui/：预览服务器（15052 → 41281）与 API 同源，API 的素材地址是 /assets/**，
  // 若前端也用 /assets 会被代理到后端，表现为 index.js 返回 JSON、页面白屏。
  build: {
    assetsDir: 'ui'
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    // 兜底端口：npm run dev 用 --port 41281 覆盖本值（AGENTS.md §8）；两处不同步时
    // `npx vite` 与 `npm run dev` 会落在不同端口，排查时容易对着旧实例看。
    port: 5173,
    allowedHosts: ['company.binnarui.com', '192.168.1.15', 'localhost'],
    // 图片/媒体统一由 API 提供：商品图 /images/**、素材 /assets/**。
    // 以前没有这两条代理，商品缩略图以 /images/x.svg 请求到 admin 自身 → 404 → 裂图。
    // 注意用 ^ 锚定：Vite 的 key 是"前缀匹配"，'/supplier' 会把前端路由 '/suppliers' 也代理到后端，
    // 于是直接打开 /suppliers 得到 JSON 404（不是页面）。
    proxy: backendProxy
  },
  preview: {
    // 15052 指向本机的 41281：用 preview 提供**构建产物**，同时保留与 dev 相同的代理，
    // 这样前端仍走同源请求（不把 API 地址烤成 localhost）。
    port: 41281,
    strictPort: true,
    allowedHosts: ['company.binnarui.com', '192.168.1.15', 'localhost'],
    proxy: backendProxy
  }
})