import { ApiClient } from '@fullmall/api-client'

// new URL(path, '') 会抛错：同源时用当前 origin，仍然走 Vite 代理
const baseUrl = import.meta.env.VITE_API_BASE || window.location.origin

export const api = new ApiClient(baseUrl)
export const appMode = import.meta.env.VITE_APP_MODE || 'dev'