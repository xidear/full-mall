<script setup lang="ts">
const route = useRoute()
const config = useRuntimeConfig()
const { token, setToken, clearToken } = useMember()
const phone = ref('')
const code = ref('')
const sending = ref(false)
const submitting = ref(false)
const error = ref('')
const notice = ref('')

async function sendCode() {
  error.value = ''
  notice.value = ''
  if (!/^1\d{10}$/.test(phone.value.trim())) {
    error.value = '请输入 11 位手机号'
    return
  }
  sending.value = true
  try {
    const issued = await $fetch<{ phone: string; debugCode?: string }>('/api/v1/member/auth/otp', {
      method: 'POST',
      baseURL: config.public.apiBase,
      body: { phone: phone.value.trim() }
    })
    if (issued.debugCode) {
      code.value = issued.debugCode
      notice.value = `演示环境验证码已填入：${issued.debugCode}`
    } else {
      notice.value = '验证码已发送，请查收短信'
    }
  } catch (cause) {
    error.value = cause instanceof Error ? '验证码发送失败，请稍后重试' : '验证码发送失败'
  } finally {
    sending.value = false
  }
}

async function submit() {
  error.value = ''
  if (!/^1\d{10}$/.test(phone.value.trim())) {
    error.value = '请输入 11 位手机号'
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
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/account'
    await navigateTo(redirect)
  } catch (cause) {
    error.value = cause instanceof Error ? '登录失败：验证码不正确或已过期' : '登录失败'
  } finally {
    submitting.value = false
  }
}

function logout() {
  clearToken()
  notice.value = '已退出登录'
}

useHead({ title: '登录 / 注册 · Full Mall' })
</script>

<template>
  <section class="login-page">
    <div class="panel login-card">
      <p class="eyebrow">MEMBER</p>
      <h1>{{ token ? '已登录' : '手机号登录 / 注册' }}</h1>
      <p class="muted">未注册手机号验证后自动创建会员账号。收藏、足迹、订单和消息都绑定该账号。</p>

      <template v-if="token">
        <div class="login-actions">
          <NuxtLink to="/account" class="button">我的订单</NuxtLink>
          <NuxtLink to="/favorites" class="button button-secondary">我的收藏</NuxtLink>
          <button type="button" class="button button-ghost" @click="logout">退出登录</button>
        </div>
      </template>

      <form v-else class="login-form" @submit.prevent="submit">
        <label>
          <span>手机号</span>
          <input v-model="phone" type="tel" inputmode="numeric" maxlength="11" autocomplete="tel" placeholder="请输入 11 位手机号">
        </label>
        <label>
          <span>短信验证码</span>
          <div class="code-row">
            <input v-model="code" type="text" inputmode="numeric" maxlength="6" autocomplete="one-time-code" placeholder="6 位验证码">
            <button type="button" class="button button-secondary" :disabled="sending" @click="sendCode">
              {{ sending ? '发送中…' : '获取验证码' }}
            </button>
          </div>
        </label>
        <p v-if="notice" class="notice-text">{{ notice }}</p>
        <p v-if="error" class="error-text">{{ error }}</p>
        <button type="submit" class="button full-width" :disabled="submitting">
          {{ submitting ? '登录中…' : '登录 / 注册' }}
        </button>
        <p class="muted login-legal">
          登录即表示同意
          <NuxtLink to="/legal/TERMS">《服务条款》</NuxtLink>
          与
          <NuxtLink to="/legal/PRIVACY">《隐私政策》</NuxtLink>
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
.login-form label { display: grid; gap: 6px; font-size: 13px; color: #555; }
.login-form input { height: 44px; border: 1px solid #d8d3c8; border-radius: 8px; padding: 0 12px; font-size: 15px; }
.code-row { display: flex; gap: 8px; }
.code-row input { flex: 1; }
.login-actions { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 20px; }
.login-legal { font-size: 12px; margin: 4px 0 0; }
.notice-text { color: #0f766e; background: #ecfdf5; border: 1px solid #99f6e4; border-radius: 8px; padding: 8px 10px; font-size: 13px; }
.error-text { color: #b91c1c; font-size: 13px; }
.full-width { width: 100%; }
</style>
