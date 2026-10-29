<script setup lang="ts">
import { apiErrorMessage } from '~/utils/apiError'

const route = useRoute()
const config = useRuntimeConfig()
const { token, setToken, clearToken } = useMember()
const { siteName } = useSite()
const { t } = useLocale()
const { identifierHint, phonePattern } = useRegionPolicy()
const phone = ref('')
const code = ref('')
const sending = ref(false)
const submitting = ref(false)
const error = ref('')
const notice = ref('')

function validPhone(value: string) {
  return phonePattern.test(value.trim())
}

async function sendCode() {
  error.value = ''
  notice.value = ''
  if (!validPhone(phone.value)) {
    error.value = '请输入正确的手机号（可带国家区号）'
    return
  }
  sending.value = true
  try {
    const issued = await $fetch<{ phone: string; debugCode?: string }>('/api/v1/member/auth/otp', {
      method: 'POST',
      baseURL: config.public.apiBase,
      body: { phone: phone.value.trim() }
    })
    // debugCode 只可能来自演示/测试环境（短信没真发，后端回显验证码并自动填入）。
    // 生产环境若返回该字段就等于免验证码登录，因此它必须由后端环境决定，前端不做开关。
    if (issued.debugCode) {
      code.value = issued.debugCode
      notice.value = `演示环境验证码已填入：${issued.debugCode}`
    } else {
      notice.value = '验证码已发送，请查收短信'
    }
  } catch (cause) {
    error.value = apiErrorMessage(cause, '验证码发送失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

async function submit() {
  error.value = ''
  if (!validPhone(phone.value)) {
    error.value = '请输入正确的手机号（可带国家区号）'
    return
  }
  if (!code.value.trim()) {
    error.value = '请输入短信验证码'
    return
  }
  submitting.value = true
  try {
    const session = await $fetch<{ token: string }>('/api/v1/member/auth/verify', {
      method: 'POST',
      baseURL: config.public.apiBase,
      body: { phone: phone.value.trim(), code: code.value.trim() }
    })
    setToken(session.token)
    const target = typeof route.query.redirect === 'string' ? route.query.redirect : '/account'
    // 只接受站内路径，避免 redirect 被拿来做开放重定向。
    const redirect = target.startsWith('/') && !target.startsWith('//') ? target : '/account'
    await navigateTo(redirect)
  } catch (cause) {
    error.value = apiErrorMessage(cause, '登录失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

async function logout() {
  try {
    if (token.value) {
      await $fetch('/api/v1/member/auth/logout', {
        method: 'POST',
        baseURL: config.public.apiBase,
        headers: { Authorization: `Bearer ${token.value}` }
      })
    }
  } finally {
    // 后端登出失败也照样清本地令牌：否则顾客会卡在“有令牌但服务端已失效”的状态。
    // 代价是令牌可能没被服务端吊销，真正失效依赖 cookie 过期（useMember 里 30 天）或后端判定。
    clearToken()
    notice.value = '已退出登录'
  }
}

useHead(() => ({ title: `登录 / 注册 · ${siteName.value}` }))
</script>

<template>
  <section class="login-page">
    <div class="panel login-card">
      <p class="eyebrow">MEMBER</p>
      <!-- 这里的“已登录”只表示本地存有令牌，并不代表令牌仍然有效：前端不解析也不校验过期时间，
           令牌被后端判定失效时，要等到下一次需要鉴权的请求才会暴露，届时按 401 重新登录。 -->
      <h1>{{ token ? '已登录' : t('login.title') }}</h1>
      <p class="muted">{{ t('login.autoAccount') }}</p>

      <template v-if="token">
        <div class="login-actions">
          <NuxtLink to="/account" class="button">我的订单</NuxtLink>
          <NuxtLink to="/favorites" class="button button-secondary">我的收藏</NuxtLink>
          <button type="button" class="button button-ghost" @click="logout">退出登录</button>
        </div>
      </template>

      <form v-else class="login-form" @submit.prevent="submit">
        <label>
          <span>{{ t('login.phone') }}</span>
          <input v-model="phone" type="tel" autocomplete="tel" placeholder="请输入手机号">
          <small class="field-hint">{{ identifierHint }}</small>
        </label>
        <label>
          <span>{{ t('login.code') }}</span>
          <div class="code-row">
            <input v-model="code" type="text" inputmode="numeric" maxlength="6" autocomplete="one-time-code" placeholder="6 位验证码">
            <button type="button" class="button button-secondary" :disabled="sending" @click="sendCode">
              {{ sending ? t('login.sending') : t('login.sendCode') }}
            </button>
          </div>
        </label>
        <p v-if="notice" class="notice-text" role="status">{{ notice }}</p>
        <p v-if="error" class="error-text" role="alert">{{ error }}</p>
        <button type="submit" class="button full-width" :disabled="submitting">
          {{ submitting ? t('login.submitting') : t('login.submit') }}
        </button>
        <p class="muted login-legal">
          {{ t('login.agree') }}
          <NuxtLink to="/legal/TERMS">《{{ t('legal.type.TERMS') }}》</NuxtLink>
          {{ t('common.and') }}
          <NuxtLink to="/legal/PRIVACY">《{{ t('legal.type.PRIVACY') }}》</NuxtLink>{{ t('login.autoCreated') }}
        </p>
      </form>
    </div>
  </section>
</template>

<style scoped>
.login-page { display: grid; place-items: center; min-height: 56vh; padding: 24px 0; }
.login-card { width: min(460px, 100%); padding: 28px; }
.login-card h1 { margin: 6px 0 10px; }
.login-form { display: grid; gap: 14px; margin-top: 20px; }
.login-form label { display: grid; gap: 6px; font-size: 13px; color: var(--muted); }
.login-form input { width: 100%; height: 44px; border: 1px solid var(--line); border-radius: 8px; padding: 0 12px; background: var(--surface); color: var(--text); font-size: 15px; }
.login-form input:focus { border-color: var(--brand); outline: none; }
.field-hint { color: var(--muted); font-size: 12px; line-height: 1.5; }
.code-row { display: flex; gap: 8px; }
.code-row input { flex: 1; }
.login-actions { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 20px; }
.login-legal { font-size: 12px; margin: 4px 0 0; line-height: 1.6; }
.notice-text { color: var(--brand-dark); background: color-mix(in srgb, var(--brand) 8%, var(--surface)); border: 1px solid color-mix(in srgb, var(--brand) 24%, var(--line)); border-radius: 8px; padding: 8px 10px; font-size: 13px; }
.error-text { color: var(--danger); font-size: 13px; }
.full-width { width: 100%; }
</style>
