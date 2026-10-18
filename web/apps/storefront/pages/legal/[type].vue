<script setup lang="ts">
import type { LegalDocument } from '~/types'

const route = useRoute()
const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const type = computed(() => String(route.params.type || 'TERMS'))
const { data: document, error } = await useFetch<LegalDocument>(`${base}/api/v1/legal/documents`, {
  query: { type, locale: 'zh-CN', market: 'CN' }
})

if (error.value || !document.value) {
  throw createError({ statusCode: 404, statusMessage: '法律文档不存在' })
}
</script>

<template>
  <article v-if="document" class="legal-page panel">
    <p class="eyebrow">{{ document.type }} · {{ document.version }} · {{ document.locale }}</p>
    <h1>{{ document.title }}</h1>
    <p class="muted">生效时间：{{ new Date(document.effectiveFrom).toLocaleString('zh-CN') }}</p>
    <div class="legal-content">{{ document.content }}</div>
    <p class="muted">内容哈希：{{ document.contentHash }}</p>
  </article>
</template>