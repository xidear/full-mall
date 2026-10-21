import { expect, test } from '@playwright/test'

const API = 'http://localhost:8080'
const SUPPLIER_ID = '33333333-3333-3333-3333-333333333333'
const SKU = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'

test('supplier sees platform return request and can accept it', async ({ page, request }) => {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: {
      username: 'admin', password: 'Admin@123456',
      captchaId: captcha.captchaId, captchaAnswer: operands[0] + operands[1]
    }
  })).json()
  const authorization = { Authorization: `Bearer ${login.token}` }

  const credential = await (await request.post(
    `${API}/admin/v1/suppliers/${SUPPLIER_ID}/credentials`, { headers: authorization })).json()

  // 造一笔订单，再由平台发起供应商退货
  const phone = `131${String(Date.now()).slice(-8)}`
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone } })
  const code = (await issued.json()).debugCode
  const member = (await (await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone, code } })).json()).token
  const checkout = await request.post(`${API}/api/v1/checkout`, {
    headers: { 'Idempotency-Key': `sr-${Date.now()}`, Authorization: `Bearer ${member}` },
    data: {
      items: [{ skuId: SKU, quantity: 1 }],
      fulfillmentMode: 'PICKUP',
      storeId: '22222222-2222-2222-2222-222222222222',
      contact: { name: '退货顾客', phone }, legalAcceptanceIds: []
    }
  })
  expect(checkout.status(), await checkout.text()).toBe(201)
  const order = await checkout.json()

  // 唯一原因：历史运行会留下同名退货单，避免 strict mode 命中多条
  const reason = `E2E 退货 ${Date.now().toString().slice(-8)}`
  const created = await request.post(`${API}/admin/v1/supplier-returns`, {
    headers: authorization,
    data: { supplierId: SUPPLIER_ID, orderId: order.id, reason, amount: 500, currency: 'CNY' }
  })
  expect(created.ok(), await created.text()).toBeTruthy()
  const returnId = (await created.json()).id as string

  // 供应商门户：连接 → 看到退货 → 同意
  await page.goto('http://localhost:5275')
  await page.getByPlaceholder('sup_...').fill(credential.accessKey)
  await page.getByPlaceholder('sec_...').fill(credential.secret)
  await page.getByRole('button', { name: '连接并校验签名' }).click()
  await expect(page.getByText(/已连接：演示供应商/)).toBeVisible()

  const item = page.locator('.return-list li', { hasText: reason })
  await expect(item).toBeVisible({ timeout: 10_000 })
  await item.getByRole('button', { name: '同意' }).click()
  await expect(page.getByText('已同意退货')).toBeVisible({ timeout: 10_000 })

  // 复核后端状态
  const list = await (await request.get(`${API}/admin/v1/supplier-returns?status=ACCEPTED`, {
    headers: authorization })).json()
  expect(list.items.some((row: { id: string }) => row.id === returnId)).toBeTruthy()
})
