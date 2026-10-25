import { expect, test } from '@playwright/test'

const API = 'http://localhost:41280'
const SKU = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
const STORE = '22222222-2222-2222-2222-222222222222'

async function adminToken(request: import('@playwright/test').APIRequestContext) {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const numbers = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: numbers[0] + numbers[1] }
  })
  return (await login.json()).token as string
}

test('exchange flows from request to completion', async ({ request }) => {
  const admin = await adminToken(request)
  const phone = `132${String(Date.now()).slice(-8)}`

  // 会员下单并支付（自提，避免地址必填）
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone } })
  const code = (await issued.json()).debugCode
  const member = (await (await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone, code } })).json()).token as string
  // 先补库存：E2E 反复运行会把演示 SKU 库存耗尽（真实约束，不是放水）
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const nums = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456', captchaId: captcha.captchaId, captchaAnswer: String(nums[0] + nums[1]) }
  })
  const token = (await login.json()).token as string
  const receipt = await request.post(`${API}/admin/v1/warehouse/receipts`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      locationType: 'WAREHOUSE',
      locationId: '11111111-1111-1111-1111-111111111111',
      note: 'E2E 补货',
      lines: [{ skuId: SKU, batchNo: `E2E-${Date.now()}`, quantity: 5, unitCostAmount: 2000, currency: 'CNY' }]
    }
  })
  expect(receipt.ok(), await receipt.text()).toBeTruthy()
  const receiptId = (await receipt.json()).id
  const confirmed = await request.post(
    `${API}/admin/v1/warehouse/receipts/${receiptId}/confirm`, { headers: { Authorization: `Bearer ${token}` } })
  expect(confirmed.ok(), await confirmed.text()).toBeTruthy()

  const checkout = await request.post(`${API}/api/v1/checkout`, {
    headers: { 'Idempotency-Key': `ex-${Date.now()}`, Authorization: `Bearer ${member}` },
    data: {
      items: [{ skuId: SKU, quantity: 1 }],
      fulfillmentMode: 'PICKUP', storeId: STORE,
      contact: { name: '换货顾客', phone }, legalAcceptanceIds: []
    }
  })
  expect(checkout.status(), await checkout.text()).toBe(201)
  const order = await checkout.json()
  expect((await request.post(`${API}/api/v1/payments/mock/${order.id}/pay`)).ok()).toBeTruthy()

  // 顾客申请换货
  const lineId = order.lines[0].id
  const created = await request.post(`${API}/api/v1/orders/${order.id}/after-sales`, {
    headers: { Authorization: `Bearer ${member}` },
    data: {
      type: 'EXCHANGE', reason: '尺码不合适', contactPhone: phone,
      items: [{ orderLineId: lineId, quantity: 1 }]
    }
  })
  expect(created.status(), await created.text()).toBe(200)
  const afterSale = await created.json()

  // 运营：审核 → 收货（隔离批次）→ 发换货 → 完成
  const approved = await request.post(`${API}/admin/v1/after-sales/${afterSale.id}/approve`, {
    headers: { Authorization: `Bearer ${admin}` }, data: { adminNote: '同意换货' }
  })
  expect(approved.ok(), await approved.text()).toBeTruthy()

  const received = await request.post(`${API}/admin/v1/after-sales/${afterSale.id}/receive`, {
    headers: { Authorization: `Bearer ${admin}` },
    data: { locationType: 'STORE', locationId: STORE }
  })
  expect(received.ok(), await received.text()).toBeTruthy()

  const shipped = await request.post(`${API}/admin/v1/after-sales/${afterSale.id}/exchange-ship`, {
    headers: { Authorization: `Bearer ${admin}` },
    data: { carrierCode: 'SF', trackingNo: 'SF-EX-E2E-1' }
  })
  expect(shipped.ok(), await shipped.text()).toBeTruthy()

  const completed = await request.post(`${API}/admin/v1/after-sales/${afterSale.id}/complete-exchange`, {
    headers: { Authorization: `Bearer ${admin}` }, data: {}
  })
  expect(completed.ok(), await completed.text()).toBeTruthy()
  const finalState = await completed.json()
  expect(finalState.status).toBe('COMPLETED')
})
