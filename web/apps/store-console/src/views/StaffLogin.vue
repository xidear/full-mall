<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { login } from '../auth'
import { api } from '../api'

const emit = defineEmits<{ authenticated: [] }>()

const form = reactive({
  username: 'store',
  password: '',
  captchaAnswer: ''
})
const captcha = ref<{ captchaId: string; question: string } | null>(null)
const captchaLoading = ref(false)
const submitting = ref(false)
const error = ref('')

onMounted(loadCaptcha)

async function loadCaptcha() {
  captchaLoading.value = true
  error.value = ''
  try {
    captcha.value = await api.get<{ captchaId: string; question: string }>('/admin/v1/auth/captcha')
    form.captchaAnswer = ''
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '验证码加载失败'
  } finally {
    captchaLoading.value = false
  }
}

async function submit() {
  if (!captcha.value) {
    error.value = '请先获取验证码'
    return
  }
  if (!form.username.trim() || !form.password) {
    error.value = '请输入账号和密码'
    return
  }
  submitting.value = true
  error.value = ''
  try {
    await login(form.username.trim(), form.password, captcha.value.captchaId, form.captchaAnswer)
    emit('authenticated')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '登录失败'
    await loadCaptcha()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-shell">
    <form class="login-card" @submit.prevent="submit">
      <div class="login-brand">
        <span class="store-mark">FM</span>
        <div>
          <h1>门店工作台登录</h1>
          <p>使用门店人员或运营账号登录，登录后可进行拣货、自提核销和收银。</p>
        </div>
      </div>

      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />

      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="captcha-row">
            <el-input v-model="form.captchaAnswer" placeholder="请输入验证码" @keyup.enter="submit" />
            <button type="button" class="captcha-question" :disabled="captchaLoading" @click="loadCaptcha">
              {{ captcha?.question || '刷新验证码' }}
            </button>
          </div>
        </el-form-item>
      </el-form>

      <el-button type="primary" native-type="submit" size="large" :loading="submitting" class="login-button">
        登录
      </el-button>
      <p class="login-hint">演示账号：store / Store@123456（生产环境请修改默认密码）</p>
    </form>
  </div>
</template>

<style scoped>
.login-shell {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background: radial-gradient(circle at top left, #2f4d3d 0, #1f2421 55%, #141815 100%);
}

.login-card {
  display: flex;
  width: min(460px, 100%);
  flex-direction: column;
  gap: 18px;
  padding: 32px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.32);
}

.login-brand {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.login-brand h1 {
  margin: 0 0 6px;
  font-size: 22px;
}

.login-brand p {
  margin: 0;
  color: #6b716c;
  font-size: 13px;
  line-height: 1.6;
}

.captcha-row {
  display: flex;
  width: 100%;
  gap: 10px;
}

.captcha-question {
  min-width: 150px;
  border: 1px solid #d8d4ca;
  border-radius: 8px;
  background: #f7f5ef;
  color: #1f2421;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.login-button {
  width: 100%;
}

.login-hint {
  margin: 0;
  color: #8a8f8a;
  font-size: 12px;
  text-align: center;
}
</style>
