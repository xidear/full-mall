<script setup lang="ts">
import type { NuxtError } from '#app'

const props = defineProps<{ error: NuxtError }>()

const title = computed(() => props.error.statusCode === 404 ? '404 · 页面不存在' : '页面暂时无法访问')
const detail = computed(() => props.error.statusMessage || props.error.message || '请稍后重试')
</script>

<template>
  <NuxtLayout>
    <div class="error-shell">
      <div class="error-card">
        <p class="eyebrow">ERROR {{ error.statusCode }}</p>
        <h1>{{ title }}</h1>
        <p class="muted">{{ detail }}</p>
        <div class="error-actions">
          <NuxtLink to="/" class="button" @click.prevent="clearError({ redirect: '/' })">回到首页</NuxtLink>
          <NuxtLink to="/products" class="button button-secondary" @click.prevent="clearError({ redirect: '/products' })">去逛商品</NuxtLink>
        </div>
      </div>
    </div>
  </NuxtLayout>
</template>

<style scoped>
.error-shell { min-height: 60vh; display: grid; place-items: center; padding: 48px 16px; }
.error-card { max-width: 560px; width: 100%; border: 1px solid #e8e4da; border-radius: 16px; background: #fff; padding: 32px; box-shadow: 0 12px 40px rgba(0, 0, 0, .06); }
.error-card h1 { margin: 8px 0 12px; font-size: 30px; }
.error-actions { display: flex; gap: 12px; margin-top: 24px; flex-wrap: wrap; }
</style>
