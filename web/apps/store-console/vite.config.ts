import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      '/api': 'http://localhost:8080',
      '/store': 'http://localhost:8080',
      '/admin': 'http://localhost:8080'
    }
  }
})