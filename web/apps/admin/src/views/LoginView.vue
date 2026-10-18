<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../auth'
import { api, appMode } from '../api'

const router = useRouter()
const route = useRoute()
const username = ref('admin')
const password = ref('')
const captchaAnswer = ref('')
const captcha = ref<{ captchaId: string; question: string } | null>(null)
const captchaLoading = ref(false)
const loading = ref(false)
const error = ref('')

onMounted(loadCaptcha)

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    captcha.value = await api.get<{ captchaId: string; question: string }>('/admin/v1/auth/captcha')
    captchaAnswer.value = ''
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '验证码加载失败'
  } finally {
    captchaLoading.value = false
  }
}

async function submit() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '请输入账号和密码'
    return
  }
  if (!captcha.value || !captchaAnswer.value.trim()) {
    error.value = '请输入验证码'
    return
  }
  loading.value = true
  try {
    await login(username.value.trim(), password.value, captcha.value.captchaId, captchaAnswer.value.trim())
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '登录失败'
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-brand">
        <span class="login-mark">FM</span>
        <div>
          <strong>Full Mall</strong>
          <small>运营后台</small>
        </div>
      </div>
      <h1>登录运营后台</h1>
      <p class="muted">商品、订单、库存、门店、主题配置统一管理。</p>

      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="账号">
          <el-input v-model="username" autocomplete="username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="password"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入密码"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="captcha-row">
            <span class="captcha-question">{{ captcha?.question || '加载中…' }}</span>
            <el-button link type="primary" :loading="captchaLoading" @click="loadCaptcha">刷新</el-button>
          </div>
          <el-input
            v-model="captchaAnswer"
            inputmode="numeric"
            placeholder="请输入验证码"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
        <el-button type="primary" class="full-width" :loading="loading" @click="submit">登录</el-button>
      </el-form>

      <div v-if="appMode !== 'prod'" class="demo-accounts">
        <strong>演示账号</strong>
        <span>超级管理员：admin / Admin@123456</span>
        <span>运营人员：operator / Operator@123456</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(225, 37, 27, 0.16), transparent 36%),
    #f4f3ef;
}

.login-panel {
  width: min(420px, 100%);
  padding: 34px;
  border: 1px solid #dedbd3;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 24px 60px rgba(31, 36, 33, 0.1);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.login-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 12px;
  background: #e1251b;
  color: #fff;
  font-weight: 800;
}

.login-brand strong,
.login-brand small {
  display: block;
}

.login-brand small {
  color: #6b716c;
  font-size: 12px;
}

h1 {
  margin: 0 0 6px;
  font-size: 26px;
}

.muted {
  margin: 0 0 22px;
  color: #6b716c;
  font-size: 13px;
}

.full-width {
  width: 100%;
}

.captcha-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  margin-bottom: 6px;
}

.captcha-question {
  color: #1f2421;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.mb-16 {
  margin-bottom: 16px;
}

.demo-accounts {
  display: grid;
  gap: 4px;
  margin-top: 22px;
  padding: 14px;
  border-radius: 12px;
  background: #fbf1e8;
  color: #7a3f17;
  font-size: 12px;
}
</style>