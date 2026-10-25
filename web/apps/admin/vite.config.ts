import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:41280',
      '/admin': 'http://localhost:41280',
      '/store': 'http://localhost:41280',
      '/supplier': 'http://localhost:41280',
      '/open': 'http://localhost:41280',
      '/mock': 'http://localhost:41280'
    }
  }
})