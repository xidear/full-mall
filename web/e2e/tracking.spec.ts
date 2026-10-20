import { expect, test } from '@playwright/test'

const API = 'http://localhost:8080'
const SKU = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
const PHONE = `137${String(Date.now()).slice(-8)}`

test('customer sees tracking on order page after shipment', async ({ page, request }) => {
  // 1) 会员身份（OTP）
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone: PHONE } })
  expect(issued.ok(), await issued.text()).toBeTruthy()
  const code = (await issued.json()).debugCode
  const session = await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone: PHONE, code } })
  const token = (await session.json()).token

  // 2) 下单 + 支付
  const checkout = await request.post(`${API}/api/v1/checkout`, {
    headers: { 'Idempotency-Key': `tracking-${Date.now()}`, Authorization: `Bearer ${token}` },
    data: {
      items: [{ skuId: SKU, quantity: 1 }],
      fulfillmentMode: 'SHIP',
      contact: { name: '轨迹验证', phone: PHONE },
      address: { province: '上海市', city: '上海市', district: '静安区', detail: '测试路 1 号' },
      legalAcceptanceIds: []
    }
  })
  expect(checkout.status(), await checkout.text()).toBe(201)
  const order = await checkout.json()
  expect((await request.post(`${API}/api/v1/payments/mock/${order.id}/pay`)).ok()).toBeTruthy()

  // 3) 员工发货（带运单号）
  const adminCaptcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const numbers = (adminCaptcha.question.match(/\d+/g) ?? []).map(Number)
  const adminLogin = await request.post(`${API}/admin/v1/auth/login`, {
    data: {
      username: 'admin', password: 'Admin@123456',
      captchaId: adminCaptcha.captchaId, captchaAnswer: numbers[0] + numbers[1]
    }
  })
  const adminToken = (await adminLogin.json()).token
  const fulfillments = await request.get(`${API}/store/v1/orders/${order.id}/fulfillments`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  })
  expect(fulfillments.ok(), await fulfillments.text()).toBeTruthy()
  const fulfillmentList = await fulfillments.json()
  const fulfillmentId = fulfillmentList[0].id
  const accept = await request.post(`${API}/store/v1/fulfillments/${fulfillmentId}/accept`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  })
  expect(accept.ok(), await accept.text()).toBeTruthy()
  const pack = await request.post(`${API}/store/v1/fulfillments/${fulfillmentId}/pack`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  })
  expect(pack.ok(), await pack.text()).toBeTruthy()
  const ship = await request.post(`${API}/store/v1/fulfillments/${fulfillmentId}/ship`, {
    headers: { Authorization: `Bearer ${adminToken}` },
    data: { carrierCode: 'SF', trackingNo: 'SF-E2E-TRACK-1' }
  })
  expect(ship.ok(), await ship.text()).toBeTruthy()

  // 4) 顾客侧接口验证：带会员身份能读到轨迹事件
  const tracking = await request.get(`${API}/api/v1/orders/${order.id}/tracking`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  expect(tracking.ok(), await tracking.text()).toBeTruthy()
  const events = await tracking.json()
  expect(events.length).toBeGreaterThan(0)
  expect(JSON.stringify(events)).toContain('已揽收')
})
