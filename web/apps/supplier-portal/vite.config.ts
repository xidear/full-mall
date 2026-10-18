import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5175,
    proxy: {
      '/api': 'http://localhost:8080',
      '/supplier': 'http://localhost:8080',
      '/open': 'http://localhost:8080'
    }
  }
})