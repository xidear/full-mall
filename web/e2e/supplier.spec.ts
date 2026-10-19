import { expect, test } from '@playwright/test'

const API = 'http://localhost:8080'
const SUPPLIER_ID = '33333333-3333-3333-3333-333333333333'
const SUPPLIER_SKU = 'bbbb0000-0000-0000-0000-0000000000c1'

test('supplier portal signs requests, accepts and ships a dropship order', async ({ page, request }) => {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: {
      username: 'admin',
      password: 'Admin@123456',
      captchaId: captcha.captchaId,
      captchaAnswer: String(operands[0] + operands[1])
    }
  })).json()
  const authorization = { Authorization: `Bearer ${login.token}` }

  const credentialResponse = await request.post(
    `${API}/admin/v1/suppliers/${SUPPLIER_ID}/credentials`,
    { headers: authorization }
  )
  expect(credentialResponse.ok()).toBeTruthy()
  const credential = await credentialResponse.json()

  const checkoutResponse = await request.post(`${API}/api/v1/checkout`, {
    data: {
      items: [{ skuId: SUPPLIER_SKU, quantity: 1 }],
      fulfillmentMode: 'SHIP',
      contact: { name: '供应商 E2E 顾客', phone: '13800000007' },
      address: { province: '上海市', city: '上海市', district: '静安区', detail: '供应商测试路 7 号' },
      legalAcceptanceIds: []
    }
  })
  expect(checkoutResponse.status()).toBe(201)
  const order = await checkoutResponse.json()
  expect((await request.post(`${API}/api/v1/payments/mock/${order.id}/pay`)).ok()).toBeTruthy()

  await page.goto('http://localhost:5275')
  await page.getByPlaceholder('sup_...').fill(credential.accessKey)
  await page.getByPlaceholder('sec_...').fill(credential.secret)
  await page.getByRole('button', { name: '连接并校验签名' }).click()
  await expect(page.getByText(/已连接：演示供应商/)).toBeVisible()

  const card = page.locator('.order-card', { hasText: order.orderNo })
  await expect(card).toBeVisible()
  await card.getByRole('button', { name: '接单' }).click()
  await expect(page.getByText(`订单 ${order.orderNo} 已接单`)).toBeVisible()

  await page.locator('.filters select').selectOption('ACCEPTED')
  const acceptedCard = page.locator('.order-card', { hasText: order.orderNo })
  await expect(acceptedCard.getByRole('button', { name: '发货' })).toBeVisible()
  await acceptedCard.getByRole('button', { name: '发货' }).click()
  await acceptedCard.locator('.ship-form input').nth(1).fill('SF-E2E-001')
  await acceptedCard.getByRole('button', { name: '确认发货' }).click()
  await expect(page.getByText(`订单 ${order.orderNo} 已发货`)).toBeVisible()

  const finalOrder = await (await request.get(`${API}/api/v1/orders/${order.id}`)).json()
  expect(finalOrder.status).toBe('FULFILLED')
})
