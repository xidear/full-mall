<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { apiErrorMessage } from '~/utils/apiError'

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

const floor = computed(() => activity.value?.floorPrice ?? 0)
function money(amount: number) {
  // 后端金额单位是分，统一交给 formatMoney 换算与格式化（见 utils/money.ts），
  // 页面里不要再除 100 或自己拼货币符号，否则与全站单位约定不一致。
  return formatMoney({ amount, currency: activity.value?.currency || 'CNY' })
}

const loggedIn = ref(false)

function memberId(): string | null {
  // 这里用的是 localStorage 里另存的会员 ID，而不是 useMember 的登录令牌：两者不同步时，
  // 会出现“站内已登录、本页提示未登录”的偏差；服务端渲染也读不到 localStorage，故 SSR 阶段恒为未登录。
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
    message.value = apiErrorMessage(cause, '发起失败，请稍后重试')
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
    message.value = apiErrorMessage(cause, '帮砍失败，请稍后重试')
  } finally {
    busy.value = false
  }
}

</script>

<template>
  <div class="bargain">
    <p v-if="error" class="error">活动加载失败：{{ apiErrorMessage(error, '请稍后重试') }}</p>
    <template v-else-if="activity">
      <h1>{{ activity.title }}</h1>
      <p class="meta">原价 {{ money(activity.listPrice) }} · 最低可到 {{ money(floor) }}</p>

      <div class="card">
        <p class="price">当前价 {{ money(session?.currentPrice ?? activity.listPrice) }}</p>
        <p class="meta">
          已砍 {{ session?.helpers ?? 0 }} 刀 / 需 {{ activity.minHelpers }} 人 ·
          每刀 {{ money(activity.cutMin) }}–{{ money(activity.cutMax) }}
        </p>
        <!-- 结束时间用浏览器本地时区（toLocaleString），没有走其它页面的会员/站点时区（memberTimezone），
             跨时区顾客看到的截止时间会与站内其它位置不一致，属于已知偏差。 -->
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
        底价 {{ money(floor) }} 为活动下限；砍到下限后不再下降。
      </p>
    </template>
  </div>
</template>

<style scoped>
.bargain { max-width: 720px; margin: 0 auto; padding: 24px 16px; }
.meta { color: var(--muted); font-size: 14px; }
.card { border: 1px solid var(--line); border-radius: 8px; padding: 16px; margin-top: 16px; }
.price { font-size: 28px; font-weight: 700; color: var(--brand); margin: 0 0 8px; }
.actions { display: flex; gap: 12px; margin-top: 12px; }
.btn { padding: 8px 18px; border-radius: 6px; border: none; background: var(--brand); color: #fff; cursor: pointer; }
.btn.ghost { background: var(--surface); color: var(--brand); border: 1px solid var(--brand); }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.warn { color: var(--brand); font-size: 13px; margin-top: 8px; }
.error { color: var(--danger); }
.note { color: var(--muted); font-size: 12px; margin-top: 12px; }
</style>
