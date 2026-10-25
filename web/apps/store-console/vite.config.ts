import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      '/api': 'http://localhost:41280',
      '/store': 'http://localhost:41280',
      '/admin': 'http://localhost:41280'
    }
  }
})