import { expect, test } from '@playwright/test'

const API = 'http://localhost:8080'
const SKU = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
const STORE = '22222222-2222-2222-2222-222222222222'
const PROMOTER = '11111111-1111-1111-1111-111111111111'

test('partial return holds commission then reverses it on refund', async ({ request }) => {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: operands[0] + operands[1] }
  })).json()
  const admin = { Authorization: `Bearer ${login.token}` }

  // 顾客带推广人下单（binding 策略：默认 PERMANENT + CART）
  const phone = `138${String(Date.now()).slice(-8)}`
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone } })
  const code = (await issued.json()).debugCode
  const member = (await (await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone, code } })).json()).token

  const checkout = await request.post(`${API}/api/v1/checkout`, {
    headers: { 'Idempotency-Key': `hold-${Date.now()}`, Authorization: `Bearer ${member}` },
    data: {
      items: [{ skuId: SKU, quantity: 2 }],
      fulfillmentMode: 'PICKUP', storeId: STORE,
      contact: { name: '佣金验证', phone },
      legalAcceptanceIds: [],
      referrerId: PROMOTER, referralMode: 'PERMANENT',
      context: { couponId: null, pointsToUse: 0, otpCode: '', placedBy: 'CUSTOMER', placedByOperator: null }
    }
  })
  expect(checkout.status(), await checkout.text()).toBe(201)
  const order = await checkout.json()
  expect((await request.post(`${API}/api/v1/payments/mock/${order.id}/pay`)).ok()).toBeTruthy()

  // 佣金已生成
  const beforeList = await (await request.get(`${API}/admin/v1/distribution/commissions`,
    { headers: admin })).json()
  const commission = beforeList.find((row: { orderId: string }) => row.orderId === order.id)
  expect(commission, '订单应产生佣金').toBeTruthy()
  expect(commission.holdAmount.amount).toBe(0)

  // 顾客申请 1 件退货
  const applied = await request.post(`${API}/api/v1/orders/${order.id}/after-sales`, {
    headers: { Authorization: `Bearer ${member}` },
    data: {
      type: 'RETURN', reason: '部分退货', contactPhone: phone,
      items: [{ orderLineId: order.lines[0].id, quantity: 1 }]
    }
  })
  expect(applied.status(), await applied.text()).toBe(200)
  const afterSale = await applied.json()

  // 冻结生效
  const heldList = await (await request.get(`${API}/admin/v1/distribution/commissions`,
    { headers: admin })).json()
  const held = heldList.find((row: { id: string }) => row.id === commission.id)
  expect(held.holdAmount.amount).toBeGreaterThan(0)

  // 运营：审核 → 收货 → 退款（售后关闭=退货完成）
  for (const [path, body] of [
    ['approve', { adminNote: '同意' }],
    ['receive', { locationType: 'STORE', locationId: STORE }]
  ] as const) {
    const response = await request.post(`${API}/admin/v1/after-sales/${afterSale.id}/${path}`,
      { headers: admin, data: body })
    expect(response.ok(), await response.text()).toBeTruthy()
  }
  const refunded = await request.post(`${API}/admin/v1/after-sales/${afterSale.id}/refund`,
    { headers: admin })
  expect(refunded.ok(), await refunded.text()).toBeTruthy()

  // 回冲生效、冻结解除
  const finalList = await (await request.get(`${API}/admin/v1/distribution/commissions`,
    { headers: admin })).json()
  const final = finalList.find((row: { id: string }) => row.id === commission.id)
  expect(final.reversedAmount.amount).toBeGreaterThan(0)
  expect(final.holdAmount.amount).toBe(0)
})
