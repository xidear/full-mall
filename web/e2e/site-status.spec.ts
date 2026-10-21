import { expect, test } from '@playwright/test'

const API = 'http://localhost:8080'

test('maintenance status blocks storefront and restores', async ({ request }) => {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: operands[0] + operands[1] }
  })).json()
  const headers = { Authorization: `Bearer ${login.token}` }

  const before = await request.get(`${API}/api/v1/products?size=1`)
  expect(before.status()).toBe(200)

  const maintenance = await request.put(`${API}/admin/v1/settings`, {
    headers,
    data: { site_status: 'MAINTENANCE', maintenance_message: '系统维护中，请稍后再试' }
  })
  expect(maintenance.ok(), await maintenance.text()).toBeTruthy()

  const blocked = await request.get(`${API}/api/v1/products?size=1`)
  expect(blocked.status()).toBe(503)
  const body = await blocked.json()
  expect(body.code).toBe('SITE_MAINTENANCE')

  const restore = await request.put(`${API}/admin/v1/settings`, {
    headers, data: { site_status: 'OPEN' }
  })
  expect(restore.ok(), await restore.text()).toBeTruthy()

  const after = await request.get(`${API}/api/v1/products?size=1`)
  expect(after.status()).toBe(200)
})
