import { reactive } from 'vue'
import type { StaffUser } from '@fullmall/api-client'
import { api } from './api'

const TOKEN_KEY = 'fullmall.staff.token'

export const auth = reactive<{
  token: string
  user: StaffUser | null
}>({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: null
})

export function loginRequired() {
  return import.meta.env.VITE_APP_MODE === 'prod'
}

export async function login(username: string, password: string, captchaId: string, captchaAnswer: string) {
  const response = await api.post<{ token: string; user: StaffUser }>('/admin/v1/auth/login', {
    username,
    password,
    captchaId,
    captchaAnswer
  })
  auth.token = response.token
  auth.user = response.user
  localStorage.setItem(TOKEN_KEY, response.token)
  api.setToken(response.token)
}

export function logout() {
  auth.token = ''
  auth.user = null
  localStorage.removeItem(TOKEN_KEY)
  api.setToken(null)
}
