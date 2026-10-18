import { expect, test } from '@playwright/test'

const API = 'http://localhost:8080'
const SKU = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
const STORE = '22222222-2222-2222-2222-222222222222'

let orderId = ''
let fulfillmentId = ''

test.beforeAll(async ({ request }) => {
  const checkout = await request.post(`${API}/api/v1/checkout`, {
    headers: { 'Idempotency-Key': `e2e-pickup-${Date.now()}` },
    data: {
      items: [{ skuId: SKU, quantity: 1 }],
      fulfillmentMode: 'PICKUP',
      storeId: STORE,
      contact: { name: 'E2E Pickup', phone: '13800000000' },
      legalAcceptanceIds: []
    }
  })
  expect(checkout.status(), await checkout.text()).toBe(201)
  const order = await checkout.json()
  orderId = order.id

  const payment = await request.post(`${API}/api/v1/payments/mock/${orderId}/pay`)
  expect(payment.ok(), await payment.text()).toBeTruthy()

  const fulfillments = await request.get(`${API}/api/v1/orders/${orderId}/fulfillments`)
  const fulfillmentList = await fulfillments.json()
  fulfillmentId = fulfillmentList[0].id
})

test('store console picks by product row and verifies pickup after loading customer info', async ({ page, request }) => {
  await page.goto('http://localhost:5274/')
  await expect(page.getByText('实时推送已连接')).toBeVisible()

  const shortId = fulfillmentId.slice(0, 8)
  const card = page.locator('.task-card', { hasText: shortId })
  await expect(card).toBeVisible()
  await expect(card.getByText('有机鲜牛奶')).toBeVisible()
  await card.getByRole('button', { name: '确认拣货' }).click()
  await expect(page.getByText(/已确认拣货/)).toBeVisible()

  const fulfillments = await request.get(`${API}/api/v1/orders/${orderId}/fulfillments`)
  const fulfillmentList = await fulfillments.json()
  const pickupCode = fulfillmentList[0].pickupCode
  expect(pickupCode).toBeTruthy()

  await page.getByPlaceholder('输入或扫描提货码').fill(pickupCode)
  await page.getByRole('button', { name: '拉取信息' }).click()
  await expect(page.getByText('E2E Pickup')).toBeVisible()
  await expect(page.getByText('有机鲜牛奶').last()).toBeVisible()
  await expect(page.getByText('已核销 0 / 共 1 次，剩余 1 次')).toBeVisible()
  await page.getByRole('button', { name: '按提货码核销' }).click()
  await expect(page.getByText('核销完成，订单已履约')).toBeVisible()

  const finalOrder = await request.get(`${API}/api/v1/orders/${orderId}`)
  const final = await finalOrder.json()
  expect(final.status).toBe('FULFILLED')
})