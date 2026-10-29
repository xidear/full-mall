import { expect, test } from '@playwright/test'
import { e2ePhone } from './utils/e2ePhone'

const API = 'http://localhost:41280'
const SUPPLIER_ID = '33333333-3333-3333-3333-333333333333'
const SUPPLIER_SKU = 'bbbb0000-0000-0000-0000-0000000000c1'

test('supplier portal shows settlement rows after platform generates one', async ({ page, request }) => {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: operands[0] + operands[1] }
  })).json()
  const admin = { Authorization: `Bearer ${login.token}` }
  const credential = await (await request.post(
    `${API}/admin/v1/suppliers/${SUPPLIER_ID}/credentials`, { headers: admin })).json()

  // 造代发订单并生成结算单
  const phone = e2ePhone()
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone } })
  const code = (await issued.json()).debugCode
  const member = (await (await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone, code } })).json()).token
  const checkout = await request.post(`${API}/api/v1/checkout`, {
    headers: { 'Idempotency-Key': `settle-${Date.now()}`, Authorization: `Bearer ${member}` },
    data: {
      items: [{ skuId: SUPPLIER_SKU, quantity: 1 }],
      fulfillmentMode: 'SHIP',
      contact: { name: '结算顾客', phone },
      address: { countryCode: 'CN', province: '上海市', city: '上海市', district: '静安区', detail: '结算路 2 号' },
      legalAcceptanceIds: []
    }
  })
  expect(checkout.status(), await checkout.text()).toBe(201)
  const order = await checkout.json()
  expect((await request.post(`${API}/api/v1/payments/mock/${order.id}/pay`)).ok()).toBeTruthy()

  const start = new Date(Date.now() - 864e5).toISOString()
  const end = new Date(Date.now() + 864e5).toISOString()
  const generated = await request.post(
    `${API}/admin/v1/suppliers/${SUPPLIER_ID}/settlements?periodStart=${encodeURIComponent(start)}&periodEnd=${encodeURIComponent(end)}`,
    { headers: admin })
  expect(generated.ok(), await generated.text()).toBeTruthy()

  // 门户：连接 → 结算面板有行
  await page.goto('http://localhost:41283')
  await page.getByPlaceholder('sup_...').fill(credential.accessKey)
  await page.getByPlaceholder('sec_...').fill(credential.secret)
  await page.getByRole('button', { name: '连接并校验签名' }).click()
  await expect(page.getByText(/已连接：演示供应商/)).toBeVisible()

  await expect(page.getByRole('heading', { name: '结算单' })).toBeVisible()
  await expect(page.locator('.settle-table tbody tr').first()).toBeVisible({ timeout: 10_000 })
  await expect(page.getByRole('button', { name: '下载 CSV' })).toBeVisible()
})