import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5175,
    proxy: {
      '/api': 'http://localhost:41280',
      '/supplier': 'http://localhost:41280',
      '/open': 'http://localhost:41280'
    }
  }
})