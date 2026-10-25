import { expect, test } from '@playwright/test'

const API = 'http://localhost:41280'
const SKU = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'

async function adminToken(request: import('@playwright/test').APIRequestContext) {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const numbers = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await request.post(`${API}/admin/v1/auth/login`, {
    data: {
      username: 'admin', password: 'Admin@123456',
      captchaId: captcha.captchaId, captchaAnswer: numbers[0] + numbers[1]
    }
  })
  return (await login.json()).token as string
}

async function memberToken(request: import('@playwright/test').APIRequestContext, phone: string) {
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone } })
  const code = (await issued.json()).debugCode
  const session = await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone, code } })
  return (await session.json()).token as string
}

test('coupon is claimable and discounts checkout', async ({ request }) => {
  const admin = await adminToken(request)
  const code = `E2E-CP-${Date.now().toString().slice(-6)}`

  // 1) 后台建券并启用
  const created = await request.post(`${API}/admin/v1/coupons/templates`, {
    headers: { Authorization: `Bearer ${admin}` },
    data: {
      code, title: 'E2E 抵扣券', type: 'AMOUNT', value: 500,
      minOrderAmount: 0, totalQuantity: 10, perMemberLimit: 1, validityDays: 30,
      startsAt: new Date().toISOString(),
      endsAt: new Date(Date.now() + 7 * 864e5).toISOString()
    }
  })
  expect(created.ok(), await created.text()).toBeTruthy()
  const templateId = (await created.json()).id as string
  const activated = await request.post(`${API}/admin/v1/coupons/templates/${templateId}/activate`, {
    headers: { Authorization: `Bearer ${admin}` }
  })
  expect(activated.ok(), await activated.text()).toBeTruthy()

  // 2) 会员领券
  const phone = `133${String(Date.now()).slice(-8)}`
  const member = await memberToken(request, phone)
  const claimed = await request.post(`${API}/api/v1/coupons/${templateId}/claim`, {
    headers: { Authorization: `Bearer ${member}` }
  })
  expect(claimed.status(), await claimed.text()).toBe(201)
  const couponId = (await claimed.json()).id as string

  // 3) 用券下单：折扣必须正好等于券面额
  const checkout = await request.post(`${API}/api/v1/checkout`, {
    headers: { 'Idempotency-Key': `coupon-${Date.now()}`, Authorization: `Bearer ${member}` },
    data: {
      items: [{ skuId: SKU, quantity: 1 }],
      fulfillmentMode: 'PICKUP',
      storeId: '22222222-2222-2222-2222-222222222222',
      contact: { name: '券验证', phone },
      legalAcceptanceIds: [],
      context: { couponId }
    }
  })
  expect(checkout.status(), await checkout.text()).toBe(201)
  const order = await checkout.json()
  expect(order.discount.amount).toBe(500)

  // 4) 券已核销，不能再次使用
  const reuse = await request.post(`${API}/api/v1/checkout`, {
    headers: { 'Idempotency-Key': `coupon-reuse-${Date.now()}`, Authorization: `Bearer ${member}` },
    data: {
      items: [{ skuId: SKU, quantity: 1 }],
      fulfillmentMode: 'PICKUP',
      storeId: '22222222-2222-2222-2222-222222222222',
      contact: { name: '券验证', phone },
      legalAcceptanceIds: [],
      context: { couponId }
    }
  })
  expect(reuse.status(), await reuse.text()).toBeGreaterThanOrEqual(400)
})
