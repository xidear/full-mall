import { ApiClient } from '@fullmall/api-client'

const baseUrl = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const api = new ApiClient(baseUrl)
export const appMode = import.meta.env.VITE_APP_MODE || 'dev'