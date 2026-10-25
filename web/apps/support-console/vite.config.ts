import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5176,
    proxy: {
      '/api': 'http://localhost:41280',
      '/support': 'http://localhost:41280',
      '/ws': {
        target: 'ws://localhost:8081',
        ws: true
      }
    }
  }
})