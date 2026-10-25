import { ApiClient } from '@fullmall/api-client'

// new URL(path, '') 会抛错：同源时用当前 origin，仍然走 Vite 代理
const baseUrl = import.meta.env.VITE_API_BASE || window.location.origin
const TOKEN_KEY = 'fullmall.staff.token'

export const api = new ApiClient(baseUrl)
api.setToken(localStorage.getItem(TOKEN_KEY))
