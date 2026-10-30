import { expect, test } from '@playwright/test'
import { API_BASE } from './env'

const API = API_BASE

/** 只读接口清单：这些是"后台一打开就会调"的接口，任何 5xx 都是真 bug。 */
const ADMIN_GETS = [
  '/admin/v1/dashboard',
  '/admin/v1/products?locale=zh-CN&page=0&size=5',
  '/admin/v1/orders?page=0&size=5',
  '/admin/v1/customers?page=0&size=5',
  '/admin/v1/after-sales?page=0&size=5',
  '/admin/v1/promotions',
  '/admin/v1/promotions/rules',
  '/admin/v1/promotions/scope-options?scope=SKU&limit=5',
  '/admin/v1/coupons/templates',
  '/admin/v1/reviews?page=0&size=5',
  '/admin/v1/audit/outbox/events?page=0&size=5',
  '/admin/v1/audit/outbox/pending',
  '/admin/v1/supplier-returns?page=0&size=5',
  '/admin/v1/suppliers',
  '/admin/v1/setup/status',
  '/admin/v1/staff',
  '/admin/v1/stores',
  '/admin/v1/notifications?limit=5',
  '/admin/v1/settings',
  '/admin/v1/permissions',
  '/admin/v1/inventory/batches?limit=5',
  '/admin/v1/distribution/report?from=2026-01-01&to=2026-12-31'
]

test('all admin read endpoints respond without server errors', async ({ request }) => {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: operands[0] + operands[1] }
  })).json()
  const headers = { Authorization: `Bearer ${login.token}` }

  const failures: string[] = []
  for (const path of ADMIN_GETS) {
    const response = await request.get(`${API}${path}`, { headers })
    if (response.status() >= 500) {
      failures.push(`${path} → ${response.status()} ${(await response.text()).slice(0, 160)}`)
    } else {
      expect(response.status(), `${path} 返回 ${response.status()}`).toBeLessThan(400)
    }
  }
  // 公开接口：不带 staff token（带了会被会员过滤器当作无效会员 token 拒绝）
  const publicGets = ['/api/v1/theme?channel=PC', '/api/v1/products?size=1', '/api/v1/categories']
  for (const path of publicGets) {
    const response = await request.get(`${API}${path}`)
    if (response.status() >= 400) {
      failures.push(`[public] ${path} → ${response.status()}`)
    }
  }
  expect(failures, failures.join('\n')).toEqual([])
})
