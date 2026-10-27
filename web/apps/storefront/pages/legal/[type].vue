<script setup lang="ts">
import { formatMemberDate, formatMemberDateTime } from '~/utils/datetime'
// 会员时区：由会员资料接口提供；未登录时为 undefined（退回浏览器本地时区）
const memberTimezone = useCookie<string | null>('fullmall_member_timezone').value ?? undefined
import type { LegalDocument } from '~/types'

const route = useRoute()
const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const type = computed(() => String(route.params.type || 'TERMS'))
const { data: document, error, pending } = await useFetch<LegalDocument>(`${base}/api/v1/legal/documents`, {
  key: `legal-${type.value}`,
  query: { type: type.value, locale: 'zh-CN', market: 'CN' }
})

useHead(() => ({ title: `${document.value?.title || type.value} · Full Mall` }))

if (error.value || (!pending.value && !document.value)) {
  throw createError({ statusCode: 404, statusMessage: '法律文档不存在' })
}
</script>

<template>
  <p v-if="!document" class="empty-state">文档加载中…</p>
  <article v-else class="legal-page panel">
    <p class="eyebrow">{{ document.type }} · {{ document.version }} · {{ document.locale }}</p>
    <h1>{{ document.title }}</h1>
    <p class="muted">生效时间：{{ formatMemberDateTime(document.effectiveFrom, memberTimezone) }}</p>
    <div class="legal-content">{{ document.content }}</div>
    <p class="muted">内容哈希：{{ document.contentHash }}</p>
  </article>
</template>