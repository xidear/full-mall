<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

interface ActivityDetail {
  id: string
  skuId: string
  skuCode: string
  title: string
  listPrice: number
  currency: string
  floorPrice: number
  maxCut: number
  minHelpers: number
  cutMin: number
  cutMax: number
  startsAt: string
  endsAt: string
  sessions: number
}

interface Session {
  id: string
  currentPrice: number
  helpers: number
  status: string
}

const route = useRoute()
const config = useRuntimeConfig()
const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
const activityId = String(route.params.id)

const { data: activity, error } = await useAsyncData<ActivityDetail>(
  `bargain-${activityId}`,
  () => $fetch<ActivityDetail>(`${base}/api/v1/bargain/activities/${activityId}`)
)

const session = ref<Session | null>(null)
const busy = ref(false)
const message = ref('')

const price = computed(() => {
  const value = session.value?.currentPrice ?? activity.value?.listPrice ?? 0
  return (value / 100).toFixed(2)
})
const floor = computed(() => ((activity.value?.floorPrice ?? 0) / 100).toFixed(2))

const loggedIn = ref(false)

function memberId(): string | null {
  if (!import.meta.client) return null
  return localStorage.getItem('fullmall-member-id')
}

onMounted(() => {
  loggedIn.value = Boolean(memberId())
})

async function start() {
  const id = memberId()
  if (!id) {
    message.value = '请先登录后再发起砍价'
    return
  }
  busy.value = true
  try {
    session.value = await $fetch<Session>(
      `${base}/api/v1/bargain/activities/${activityId}/start`, {
        method: 'POST',
        query: { memberId: id }
      })
    message.value = ''
  } catch (cause) {
    message.value = cause instanceof Error ? cause.message : '发起失败'
  } finally {
    busy.value = false
  }
}

async function help() {
  const id = memberId()
  if (!id || !session.value) {
    message.value = '请先登录并已发起砍价'
    return
  }
  busy.value = true
  try {
    session.value = await $fetch<Session>(
      `${base}/api/v1/bargain/sessions/${session.value.id}/help`, {
        method: 'POST',
        query: { helperMemberId: id }
      })
    message.value = ''
  } catch (cause) {
    message.value = cause instanceof Error ? cause.message : '帮砍失败'
  } finally {
    busy.value = false
  }
}

</script>

<template>
  <div class="bargain">
    <p v-if="error" class="error">活动加载失败：{{ error.message }}</p>
    <template v-else-if="activity">
      <h1>{{ activity.title }}</h1>
      <p class="meta">原价 ¥{{ (activity.listPrice / 100).toFixed(2) }} · 最低可到 ¥{{ floor }}</p>

      <div class="card">
        <p class="price">当前价 ¥{{ price }}</p>
        <p class="meta">
          已砍 {{ session?.helpers ?? 0 }} 刀 / 需 {{ activity.minHelpers }} 人 ·
          每刀 ¥{{ (activity.cutMin / 100).toFixed(2) }}–{{ (activity.cutMax / 100).toFixed(2) }}
        </p>
        <p class="meta">结束时间：{{ new Date(activity.endsAt).toLocaleString() }}</p>

        <div class="actions">
          <button type="button" class="btn" :disabled="busy" @click="start">
            {{ session ? '重新发起' : '发起砍价' }}
          </button>
          <button type="button" class="btn ghost" :disabled="busy || !session" @click="help">
            帮砍一刀
          </button>
        </div>
        <p v-if="!loggedIn" class="warn">未登录：可查看活动，发起与帮砍需先登录。</p>
        <p v-if="message" class="warn">{{ message }}</p>
      </div>

      <p class="note">
        底价 ¥{{ floor }} 为活动下限；砍到下限后不再下降。
      </p>
    </template>
  </div>
</template>

<style scoped>
.bargain { max-width: 720px; margin: 0 auto; padding: 24px 16px; }
.meta { color: #666; font-size: 14px; }
.card { border: 1px solid #eee; border-radius: 8px; padding: 16px; margin-top: 16px; }
.price { font-size: 28px; font-weight: 700; color: #d4380d; margin: 0 0 8px; }
.actions { display: flex; gap: 12px; margin-top: 12px; }
.btn { padding: 8px 18px; border-radius: 6px; border: none; background: #d4380d; color: #fff; cursor: pointer; }
.btn.ghost { background: #fff; color: #d4380d; border: 1px solid #d4380d; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.warn { color: #d4380d; font-size: 13px; margin-top: 8px; }
.error { color: #cf1322; }
.note { color: #999; font-size: 12px; margin-top: 12px; }
</style>
