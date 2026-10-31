import { expect, test } from '@playwright/test'
import { API_BASE } from './env'

const API = API_BASE

async function adminToken(request: import('@playwright/test').APIRequestContext) {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const numbers = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await request.post(`${API}/admin/v1/auth/login`, {
    data: {
      username: 'admin', password: 'Admin@123456',
      captchaId: captcha.captchaId, captchaAnswer: String(numbers[0] + numbers[1])
    }
  })
  expect(login.ok(), await login.text()).toBeTruthy()
  return (await login.json()).token as string
}

test('币种小数位与精确换算（含 TWD/HUF 修正）', async ({ request }) => {
  const precision = await request.get(`${API}/api/v1/currencies/precision`)
  expect(precision.ok(), await precision.text()).toBeTruthy()
  const body = await precision.json()
  expect(body.defaultMinorUnits).toBe(2)
  expect(body.overrides.JPY).toBe(0)
  expect(body.overrides.KWD).toBe(3)
  // 旧实现误把 TWD/HUF 写成 0，会让金额放大 100 倍
  expect(body.overrides.TWD ?? 2).toBe(2)
  expect(body.overrides.HUF ?? 2).toBe(2)
})

test('汇率：新增 → 换算 → 删除（清理测试数据）', async ({ request }) => {
  const token = await adminToken(request)
  const effectiveAt = new Date().toISOString()
  const created = await request.put(`${API}/admin/v1/exchange-rates`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      baseCurrency: 'USD', quoteCurrency: 'CNY', rate: 7.2,
      source: 'PLAYWRIGHT', effectiveAt
    }
  })
  expect(created.status(), await created.text()).toBe(201)
  const rate = await created.json()

  const converted = await request.get(
    `${API}/admin/v1/exchange-rates/convert?amount=10000&from=USD&to=CNY`,
    { headers: { Authorization: `Bearer ${token}` } })
  expect(converted.ok(), await converted.text()).toBeTruthy()
  const conversion = await converted.json()
  expect(conversion.convertedAmount).toBe(72000)
  expect(conversion.rate).toBe(7.2)

  const deleted = await request.delete(`${API}/admin/v1/exchange-rates/${rate.id}`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  expect(deleted.status()).toBe(204)
})

test('门店列表支持 includeDeleted（软删除筛选）', async ({ request }) => {
  const token = await adminToken(request)
  const list = await request.get(
    `${API}/admin/v1/stores?includeInactive=true&includeDeleted=true`,
    { headers: { Authorization: `Bearer ${token}` } })
  expect(list.ok(), await list.text()).toBeTruthy()
  const rows = await list.json()
  expect(Array.isArray(rows)).toBeTruthy()
  for (const row of rows) {
    expect(typeof row.code).toBe('string')
    expect(typeof row.active).toBe('boolean')
  }
})