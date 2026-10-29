<script setup lang="ts">
import { formatMemberDate, formatMemberDateTime } from '~/utils/datetime'
// 会员时区优先，未登录时用站点默认时区（避免 SSR 与浏览器显示不一致）。
const memberTimezone = useDisplayTimeZone()
import { computed, ref } from 'vue'

interface MemberNotification {
  id: string
  eventType: string
  title: string
  content: string
  channel: string
  readAt: string | null
  createdAt: string
}

const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const memberToken = useCookie<string | null>('fullmall_member_token').value ?? ''
const headers: Record<string, string> = memberToken ? { Authorization: `Bearer ${memberToken}` } : {}

const items = ref<MemberNotification[]>([])
const loading = ref(false)
const error = ref('')
const markingId = ref('')

const unread = computed(() => items.value.filter(item => !item.readAt).length)

async function load() {
  if (!memberToken) return
  loading.value = true
  error.value = ''
  try {
    // 只取最近 50 条，没有分页也没有轮询/推送：更新的消息要重新进入页面（刷新）才会出现。
    items.value = await $fetch<MemberNotification[]>(`${base}/api/v1/notifications`, {
      headers, query: { limit: 50 }
    })
  } catch (cause) {
    error.value = apiErrorMessage(cause, '消息加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function markRead(item: MemberNotification) {
  if (markingId.value || item.readAt) return
  markingId.value = item.id
  try {
    await $fetch(`${base}/api/v1/notifications/${item.id}/read`, { method: 'POST', headers })
    item.readAt = new Date().toISOString()
  } catch (cause) {
    error.value = apiErrorMessage(cause, '标记已读失败，请稍后重试')
  } finally {
    markingId.value = ''
  }
}

// 只在浏览器端加载：SSR 阶段不取消息，首屏由下面的“未登录 / 加载中”分支承接。
// 未登录时 load() 直接返回且 loading 不会置 true，所以登录提示不会与“加载失败”混在一起。
if (process.client) {
  await load()
}
</script>

<template>
  <section class="page">
    <div class="head">
      <h1>消息中心</h1>
      <span v-if="memberToken" class="muted">未读 {{ unread }} 条</span>
    </div>

    <p v-if="!memberToken" class="hint">登录后可查看消息。</p>
    <p v-else-if="loading" class="hint">加载中…</p>
    <p v-else-if="error" class="hint error">消息加载失败，请稍后再试。</p>
    <EmptyState v-else-if="!items.length" icon="box" title="暂时没有消息" description="订单、售后和促销进展会出现在这里。" />

    <ul v-else class="message-list">
      <li v-for="item in items" :key="item.id" :class="{ unread: !item.readAt }">
        <div class="title-row">
          <strong>{{ item.title }}</strong>
          <span class="muted">{{ formatMemberDateTime(item.createdAt, memberTimezone) }}</span>
        </div>
        <p>{{ item.content }}</p>
        <button v-if="!item.readAt" type="button" :disabled="markingId === item.id" @click="markRead(item)">
          {{ markingId === item.id ? '处理中…' : '标记已读' }}
        </button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.page { max-width: 720px; margin: 0 auto; padding: 24px 16px; }
.head { display: flex; align-items: baseline; justify-content: space-between; }
.hint { color: var(--muted); }
.error { color: var(--danger); }
.message-list { list-style: none; padding: 0; display: grid; gap: 12px; }
.message-list li { padding: 12px 16px; border: 1px solid var(--line); border-radius: 10px; }
.message-list li.unread { border-color: var(--brand); background: var(--surface-muted); }
.title-row { display: flex; justify-content: space-between; gap: 12px; }
.muted { color: var(--muted); font-size: 13px; }
button { margin-top: 8px; padding: 4px 12px; border: 1px solid var(--line); border-radius: 6px; background: var(--surface); cursor: pointer; }
</style>
