import { ApiClient } from '@fullmall/api-client'

const baseUrl = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const TOKEN_KEY = 'fullmall.staff.token'

export const api = new ApiClient(baseUrl)
api.setToken(localStorage.getItem(TOKEN_KEY))
