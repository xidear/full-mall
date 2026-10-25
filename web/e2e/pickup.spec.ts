import { expect, test } from '@playwright/test'

const API = 'http://localhost:41280'
const SKU = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
const STORE = '22222222-2222-2222-2222-222222222222'

let orderId = ''
let fulfillmentId = ''

// 每次运行用不同手机号：OTP 有限流（同一号码 10 分钟 5 次）
const PHONE = `139${String(Date.now()).slice(-8)}`
let memberToken = ''

test.beforeAll(async ({ request }) => {
  // 下单要求"真人"：先走 OTP 拿到会员 token（dev 环境会回传验证码）
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone: PHONE } })
  expect(issued.ok(), await issued.text()).toBeTruthy()
  const code = (await issued.json()).debugCode
  expect(code, '开发环境应回传验证码').toBeTruthy()

  const session = await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone: PHONE, code } })
  expect(session.ok(), await session.text()).toBeTruthy()
  memberToken = (await session.json()).token

  const checkout = await request.post(`${API}/api/v1/checkout`, {
    headers: { 'Idempotency-Key': `e2e-pickup-${Date.now()}`, Authorization: `Bearer ${memberToken}` },
    data: {
      items: [{ skuId: SKU, quantity: 1 }],
      fulfillmentMode: 'PICKUP',
      storeId: STORE,
      contact: { name: 'E2E Pickup', phone: PHONE },
      legalAcceptanceIds: []
    }
  })
  expect(checkout.status(), await checkout.text()).toBe(201)
  const order = await checkout.json()
  orderId = order.id

  const payment = await request.post(`${API}/api/v1/payments/mock/${orderId}/pay`)
  expect(payment.ok(), await payment.text()).toBeTruthy()

  // 订单已绑定会员：用会员身份查履约（不再依赖手机号）
  const fulfillments = await request.get(`${API}/api/v1/orders/${orderId}/fulfillments`,
    { headers: { Authorization: `Bearer ${memberToken}` } })
  expect(fulfillments.ok(), await fulfillments.text()).toBeTruthy()
  fulfillmentId = (await fulfillments.json())[0].id
})

test('store console picks by product row and verifies pickup after loading customer info', async ({ page, request }) => {
  await page.goto('http://localhost:41282/')
  await expect(page.getByText('实时推送已连接')).toBeVisible()

  const shortId = fulfillmentId.slice(0, 8)
  const card = page.locator('.task-card', { hasText: shortId })
  await expect(card).toBeVisible()
  await expect(card.getByText('有机鲜牛奶')).toBeVisible()
  await card.getByRole('button', { name: '确认拣货' }).click()
  await expect(page.getByText(/已确认拣货/)).toBeVisible()

  const fulfillments = await request.get(`${API}/api/v1/orders/${orderId}/fulfillments`,
    { headers: { Authorization: `Bearer ${memberToken}` } })
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

  // 订单已绑定会员：查询最终状态要带会员身份
  const finalOrder = await request.get(`${API}/api/v1/orders/${orderId}`,
    { headers: { Authorization: `Bearer ${memberToken}` } })
  const final = await finalOrder.json()
  expect(final.status).toBe('FULFILLED')
})