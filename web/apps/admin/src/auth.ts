import { reactive } from 'vue'
import type { StaffUser } from '@fullmall/api-client'
import { api } from './api'

const TOKEN_KEY = 'fullmall.admin.token'

export const auth = reactive<{
  token: string
  user: StaffUser | null
}>({
  token: '',
  user: null
})

export function initAuth() {
  const token = localStorage.getItem(TOKEN_KEY) || ''
  auth.token = token
  api.setToken(token || null)
}

export async function login(username: string, password: string, captchaId: string, captchaAnswer: string) {
  const response = await api.post<{ token: string; user: StaffUser }>('/admin/v1/auth/login', {
    username,
    password,
    captchaId,
    captchaAnswer
  })
  setToken(response.token)
  auth.user = response.user
  return response.user
}

export async function loadMe() {
  if (!auth.token) {
    return null
  }
  auth.user = await api.get<StaffUser>('/admin/v1/auth/me')
  return auth.user
}

export async function logout() {
  try {
    if (auth.token) {
      await api.post('/admin/v1/auth/logout')
    }
  } finally {
    clearAuth()
  }
}

export async function changePassword(oldPassword: string, newPassword: string) {
  await api.post('/admin/v1/auth/change-password', { oldPassword, newPassword })
  clearAuth()
}

export function clearAuth() {
  auth.token = ''
  auth.user = null
  localStorage.removeItem(TOKEN_KEY)
  api.setToken(null)
}

function setToken(token: string) {
  auth.token = token
  localStorage.setItem(TOKEN_KEY, token)
  api.setToken(token)
}